package pers.ljy.nga.stock.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pers.ljy.nga.stock.constant.Author;
import pers.ljy.nga.stock.constant.UrlConstant;
import pers.ljy.nga.stock.util.Util;

public class PullMain {

	Logger logger = LoggerFactory.getLogger(getClass());

	private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(0, 30, 60L, TimeUnit.SECONDS,
			new SynchronousQueue<>());
	private static final String TOTAL_PAGE = "$.totalPage";

	private static final String V_ROWS = "$.vrows";

	private static final String QUOTE_PATTERN_str = "\\[quote\\](.+)\\[\\/quote\\]";

	private static final String IMG_PATTERN_Str = "\\[img\\](.+?)\\[\\/img\\]";

	private static final String B_PATTERN_str = "<b>.+<\\/b>";

	private static final String BR_PATTERN_str = "<br\\/>";

	private static final String PID_PATTERN_str = "\\[pid=(.*)\\[(.*)pid]";

	private static final String URL_PATTERN_str = "\\[url\\](.+?)\\[\\/url\\]";

	private static final Pattern IMG_PATTERN = Pattern.compile("\\[img\\](.+?)\\[\\/img\\]");

	private static final Pattern QUOTE_PATTERN = Pattern.compile(QUOTE_PATTERN_str);
	private int staticcurrentPage = 1;

	private int staticcurrentFloor = 1;

	private String tid;

	private String name;

	public PullMain(String tid, String name) {
		this.tid = tid;
		this.name = name;
	}

	public Object fetchData(int page) throws ClientProtocolException, IOException {
		Map<String, String> param = new HashMap<>();
		param.put("tid", tid);
		param.put("page", page + "");
		try {
			String allRs = Util.post(UrlConstant.URL, UrlConstant.HEADERS, param);
			Configuration conf = Configuration.defaultConfiguration();
			Object document = conf.jsonProvider().parse(allRs);
			return document;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void listenNewMessage() throws ClientProtocolException, IOException {
		Object obj = fetchData(staticcurrentPage);
		if (obj == null) {
			return;
		}
		try {
			Integer code = JsonPath.read(obj, "$.code");
			if (code == null || code.intValue() != 0) {
				return;
			}
		} catch (Exception e) {
			logger.error("code都没取到，是取了个什么鬼东西：" + obj);
			return;
		}

//		Object result = JsonPath.read(obj, "$.result");
		int length = 0;
		try {
			length = JsonPath.read(obj, "$.result.size()");
		} catch (NullPointerException e) {
			logger.error("length = JsonPath.read(obj, \"$.result.size()\") 空指针了", e);
		} catch (PathNotFoundException e2) {
			logger.error("没取到数据");
		}
		try {
			int currentPage = JsonPath.read(obj, "$.currentPage");
			int totalPage = JsonPath.read(obj, TOTAL_PAGE);
			if (length == 0) {
				logger.info("当前页为：" + staticcurrentPage + " 取到的result值为0" + ",结果中的当前页是："
						+ JsonPath.read(obj, "$.currentPage") + "结果中的总页面是：" + JsonPath.read(obj, TOTAL_PAGE));
				logger.info("值为：" + obj);
				return;
			}
			int lou = JsonPath.read(obj, "$.result[-1].lou");
			if (currentPage == totalPage) {
				if (staticcurrentFloor == lou) {
//					System.out.println(tid+"当前楼层" + staticcurrentFloor + "，没有新内容");
					return;
				}
				handleMessage(obj, lou);
			} else {
				logger.info("该翻页了,当前楼：{}", staticcurrentPage);
				logger.info("当前楼层：" + staticcurrentFloor);
				logger.info("当前消息楼：" + lou);
				if (!(lou + "").endsWith("9")) {
					final int syncPage = staticcurrentPage;
					final int syncfloor = staticcurrentPage * 20 - 1;
					final int qfloor = lou + 1;
					// update 1.13:新增补偿被抽楼的楼层
					logger.info("消息不是以9结尾就翻页了，理论最后一楼：{},等待补偿。", syncfloor);
					THREAD_POOL.execute(() -> {
						try {
							TimeUnit.SECONDS.sleep(30);
							Object syncObj = fetchData(syncPage);
							int nowFloor = JsonPath.read(syncObj, "$.result[-1].lou");
							logger.info("异步再次获取{}页的消息，现在的楼是{}", syncPage, nowFloor);
							for (int i = qfloor; i <= syncfloor; i++) {
								Object content = JsonPath.read(syncObj, "$.result[?(@.lou ==" + i + ")]");
								sendMessage(content, syncObj);
							}
						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					});
				}
				if (staticcurrentFloor == lou) {
					this.staticcurrentPage += 1;
				} else if (staticcurrentFloor > lou) {
					// 复位，理论上当前楼层不可能大于当前消息的楼数
					this.staticcurrentFloor = lou;
					this.staticcurrentPage += 1;
				} else {
					handleMessage(obj, lou);
					this.staticcurrentPage += 1;
				}
			}
		} catch (PathNotFoundException e) {
			System.out.println("PathNotFoundException.obj is:" + obj);
		}

	}

	public void handleMessage(Object result, int lou) throws ClientProtocolException, IOException {
		for (int i = staticcurrentFloor + 1; i <= lou; i++) {
			Object content = JsonPath.read(result, "$.result[?(@.lou ==" + i + ")]");
//			System.out.println(i+"楼的消息："+content);
			staticcurrentFloor = i;
			sendMessage(content, result);

		}
	}

	public void sendMessage(Object content, Object result) throws ClientProtocolException, IOException {
		try {
			JSONArray json = (JSONArray) content;
			if (json.size() == 0) {
				logger.info("sendMessage中: " + staticcurrentFloor + "楼了，但是没取到消息，不处理");
//				staticcurrentFloor+=1;
				return;
			}
			// 该楼的具体内容
			Map json0 = (Map) json.get(0);
			Map<String, Object> author = (Map<String, Object>) json0.get("author");
			Integer uid = (Integer) author.get("uid");
			String authorName = Author.authors.get(uid);
			if (StringUtils.isEmpty(authorName)) {
				return;
			}
			String postTime = (String) json0.get("postdate");
			String contentStr = (String) json0.get("content");
			Integer pid = (Integer) json0.get("pid");
			// 修改url链接格式
			contentStr = excludeUrl(contentStr);
			// 获取引用
			String replyContent = resolveReply(contentStr);
			// 去掉引用
			contentStr = excludeQuote(contentStr);
			// 去掉回复
			contentStr = excludeReply(contentStr);

			// 处理图片
			List<String> img_url = new ArrayList<>();
			Matcher matcher = IMG_PATTERN.matcher(contentStr);
			while (matcher.find()) {
				img_url.add(matcher.group(1));
			}
			contentStr = excludeImg(contentStr);
			StringBuilder sb = new StringBuilder();
			sb.append("#### " + name + "\n");
			sb.append("#### ");
			sb.append(json0.get("lou"));
			sb.append("楼\n\n");
			sb.append("时间:");
			sb.append(postTime + "\n\n");
			sb.append("#### 作者:");
			sb.append(authorName + "\n");
			if (!StringUtils.isEmpty(replyContent)) {
				sb.append("> ");
				sb.append(replyContent + "\n\n");
			}
			sb.append("#### ");
			sb.append(contentStr);
			if (img_url.size() > 0) {
				img_url.forEach(img -> {
					sb.append("![](");
					sb.append(img);
					sb.append(")\n");
				});
			}
			sb.append("\n\n");
			sb.append("#### \n\n");
			sb.append("[点击查看原文](https://bbs.nga.cn/read.php?tid=" + tid + "&pid=" + pid + ")");
			JSONObject jsonParam = new JSONObject();
			JSONObject text = new JSONObject();
			jsonParam.put("msgtype", "markdown");
			jsonParam.put("markdown", text);
			text.put("title", "新消息");
			text.put("text", sb.toString());
			String rs = Util.postByJson(UrlConstant.DINGDING_URL, jsonParam);
		} catch (NullPointerException e) {
		}
	}

	private String excludeReply(String contentStr) {
		String rs = contentStr.replaceAll("<b>Reply to(.+?)<br/>", "");
		return rs;
	}

	private String excludeImg(String contentStr) {
		// 把图片内容去掉
		String rs = contentStr.replaceAll(IMG_PATTERN_Str, "");
		return rs;
	}

	// 处理主消息
	private String excludeQuote(String contentStr) {
		// 把引用内容去掉
		String rs = contentStr.replaceAll(QUOTE_PATTERN_str, "");
		// update:把用br分段的字段也放大
		rs = rs.replaceAll(BR_PATTERN_str, "\n #### ");
		return rs;
	}

	// 处理回复
	private String excludeUrl(String contentStr) {
		return contentStr.replaceAll(URL_PATTERN_str, "[外部链接]($1)");
	}

//TODO
	// 处理回复的消息
	private String resolveReply(String contentStr) {
		Matcher matcher = QUOTE_PATTERN.matcher(contentStr);
		if (matcher.find()) {
			try {
				String replyContent = matcher.group(1);
				replyContent = replyContent.replaceAll(IMG_PATTERN_Str, "[图片]($1)");
				replyContent = replyContent.replaceAll(BR_PATTERN_str, "");
				replyContent = replyContent.replaceAll(PID_PATTERN_str, "");
				replyContent = replyContent.replaceAll(B_PATTERN_str, "");
				return replyContent;
			} catch (Exception e) {
//				logger.error("有reply，但是报错了");
			}

		}
		return null;
	}

	public void init() throws ClientProtocolException, IOException, InterruptedException {
		while (staticcurrentPage == 1 && staticcurrentFloor == 1) {
			try {
				Object document = fetchData(9999999);
				staticcurrentPage = JsonPath.read(document, TOTAL_PAGE);
				staticcurrentFloor = (Integer) JsonPath.read(document, V_ROWS) - 1;
			} catch (Exception e) {
				TimeUnit.SECONDS.sleep(5);
			}
		}
		logger.info("{} init success.tid:{}", name, tid);
		new Thread(() -> {
			while (true) {
				try {
					listenNewMessage();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, name).start();
	}

}
