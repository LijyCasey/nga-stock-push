package pers.ljy.nga.stock.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

	private static final String TOTAL_PAGE = "$.totalPage";

	private static final String V_ROWS = "$.vrows";

	private static final String QUOTE_PATTERN_str = "\\[quote\\](.+)\\[\\/quote\\]";

	private static final String IMG_PATTERN_Str = "\\[img\\].+?\\[\\/img\\]";

	private static final String B_PATTERN_str = "<b>.+<\\/b>";

	private static final String BR_PATTERN_str = "<br\\/>";

	private static final String PID_PATTERN_str = "\\[pid=(.*)\\[(.*)pid]";

	private static final Pattern IMG_PATTERN = Pattern.compile("\\[img\\](.+?)\\[\\/img\\]");

	private static final Pattern QUOTE_PATTERN = Pattern.compile(QUOTE_PATTERN_str);
	private int staticcurrentPage = 1;

	private int staticcurrentFloor = 1;

	private String tid;

	private String sendUrl;
	
	
	public PullMain(String tid, String sendUrl) {
		this.tid = tid;
		this.sendUrl = sendUrl;
	}

	public Object fetchData(int page) throws ClientProtocolException, IOException {
		Map<String, String> param = new HashMap<>();
		param.put("tid", tid);
		param.put("page", page + "");
		String allRs = Util.post(UrlConstant.URL, UrlConstant.HEADERS, param);
		Configuration conf = Configuration.defaultConfiguration();
		Object document = conf.jsonProvider().parse(allRs);
		return document;
	}

	public void listenNewMessage() throws ClientProtocolException, IOException {
		Object obj = fetchData(staticcurrentPage);
//		Object result = JsonPath.read(obj, "$.result");
		int length = 0;
		try {
			length = JsonPath.read(obj, "$.result.size()");
		} catch (NullPointerException e) {
			System.out.println("length = JsonPath.read(obj, \"$.result.size()\") 空指针了");
		}
		try {
			int currentPage = JsonPath.read(obj, "$.currentPage");
			int totalPage = JsonPath.read(obj, TOTAL_PAGE);
			if (length == 0) {
				System.out.println("当前页为：" + staticcurrentPage + " 取到的result值为0" + ",结果中的当前页是："
						+ JsonPath.read(obj, "$.currentPage") + "结果中的总页面是：" + JsonPath.read(obj, TOTAL_PAGE));
				System.out.println("值为：" + obj);
				return;
			}
			int lou = JsonPath.read(obj, "$.result[" + (length - 1) + "].lou");
			if (currentPage == totalPage) {
				if (staticcurrentFloor == lou) {
//					System.out.println(tid+"当前楼层" + staticcurrentFloor + "，没有新内容");
					return;
				}
				handleMessage(obj, lou);
			} else {
				System.out.println("该翻页了.");
				System.out.println("当前楼层：" + staticcurrentFloor);
				System.out.println("当前消息楼：" + lou);
				System.out.println();
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
				System.out.println("sendMessage中: " + staticcurrentFloor + "楼了，但是没取到消息，不处理");
				System.out.println("sendMessage中: 结果中的当前页是：" + JsonPath.read(result, "$.currentPage") + "结果中的总页面是："
						+ JsonPath.read(result, "TOTAL_PAGE"));
				// 被抽楼了 +1楼 //8.3 9.26
//				staticcurrentFloor+=1;
				return;
			}
			Map json0 = (Map) json.get(0);
			Map<String, Object> author = (Map<String, Object>) json0.get("author");
			Integer uid = (Integer) author.get("uid");
			String authorName = Author.authors.get(uid);
			if (StringUtils.isEmpty(authorName)) {
				return;
			}
			String postTime = (String) json0.get("postdate");
			String contentStr = (String) json0.get("content");
			// 获取引用
			String replyContent = resolveReply(contentStr);
			// 去掉引用
			contentStr = excludeQuote(contentStr);
			// 处理图片
			List<String> img_url = new ArrayList<>();
			Matcher matcher = IMG_PATTERN.matcher(contentStr);
			while (matcher.find()) {
				img_url.add(matcher.group(1));
			}
			contentStr = excludeImg(contentStr);
			// 修改为markdown语法
			// 2020.8.18
			/* 
			 * #### xx楼
			 * 作者：xx
			 * > 引用内容
			 * #### 回复
			 * [图片][图片]
			 * */
			StringBuilder sb = new StringBuilder();
			sb.append("#### ");
			sb.append(json0.get("lou"));
			sb.append("楼\n\n");
			sb.append("时间:");
			sb.append(postTime + "\n\n");
			sb.append("#### 作者:");
			sb.append(authorName + "\n");
			if (!StringUtils.isEmpty(replyContent)) {
				sb.append("> ");
				sb.append(replyContent + "\n");
			}
			sb.append("#### ");
			sb.append(contentStr);
			if (img_url.size()>0) {
				img_url.forEach(img->{
					sb.append("![](");
					sb.append(img);
					sb.append(")\n");
				});
			}
			sb.append("#### \n\n");
			sb.append("#### \n\n");
			sb.append("#### \n\n");
			sb.append("[点击查看原文链接](https://bbs.nga.cn/read.php?tid=" + tid + "&page=" + staticcurrentPage+")");
			JSONObject jsonParam = new JSONObject();
			JSONObject text = new JSONObject();
			jsonParam.put("msgtype", "markdown");
			jsonParam.put("markdown", text);
//			jsonParam.put("msgtype", "text");
			text.put("title", "新消息");
			text.put("text", sb.toString());
//			text.put("content", sb.toString());
//			jsonParam.put("text", text);
			String rs = Util.postByJson(sendUrl, jsonParam);
		} catch (NullPointerException e) {
		}
	}

	private String excludeImg(String contentStr) {
		// 把图片内容去掉
		String rs = contentStr.replaceAll(IMG_PATTERN_Str, "");
		return rs;
	}

	// 处理主消息
	private static String excludeQuote(String contentStr) {
		// 把引用内容去掉
		String rs = contentStr.replaceAll(QUOTE_PATTERN_str, "");
		rs = rs.replaceAll(BR_PATTERN_str, "\n");
		return rs;
	}

	// 处理回复的消息
	private static String resolveReply(String contentStr) {
		Matcher matcher = QUOTE_PATTERN.matcher(contentStr);
		if (matcher.find()) {
			try {
				String replyContent = matcher.group(1);
				replyContent = replyContent.replaceAll(IMG_PATTERN_Str, "[图片]");
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

	public void init(String threadname) throws ClientProtocolException, IOException {
		System.out.println("init success.tid=" + tid + " sendUrl=" + sendUrl);
		Object document = fetchData(9999999);
		staticcurrentPage = JsonPath.read(document, TOTAL_PAGE);
		staticcurrentFloor = (Integer) JsonPath.read(document, V_ROWS) - 1;
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
		}, threadname).start();
	}

	public static void main(String[] args) throws ClientProtocolException, IOException {
//		PullMain main = new PullMain(UrlConstant.TID, UrlConstant.DING_URL);
//		main.init("mainFloor");
//		PullMain qiaoPull = new PullMain(UrlConstant.QIAO_TID, UrlConstant.QIAO_DING_URL);
//		qiaoPull.init("qiaobangzhu");
		String sendUrl = "https://oapi.dingtalk.com/robot/send?access_token=23a218871239635808764314bf05c617a692c8d2d1066647617f42ac5ae4516b";
//		Map<String, String> param = new HashMap<>();
//		param.put("tid", "" + 21729074);
//		param.put("page", "" + 3036);
//		String allRs = Util.post(UrlConstant.URL, UrlConstant.HEADERS, param);
//		Configuration conf = Configuration.defaultConfiguration();
//		Object document = conf.jsonProvider().parse(allRs);
//		String str = (String) JsonPath.read(document, "$.result[19].content");
//		System.out.println((Object) JsonPath.read(document, "$.result"));
//		String str = "[quote][pid=445489970,21729074,3036]Reply[\\/pid] <b>Post by [uid=17316127]夜冉OK[\\/uid] (2020-08-16 17:07):<\\/b><br\\/><br\\/>还木有消息。[img]http:\\/\\/img.nga.178.com\\/attachments\\/mon_201209\\/14\\/-47218_5052bc4cc6331.png[\\/img]感觉快了吧，信号弹已经打出来了，疫情不大规模反弹，院线要复苏了。感觉最近电影相关股票都有可能走一波。[img]http:\\/\\/img.nga.178.com\\/attachments\\/mon_201212\\/24\\/-1324875_50d841a63a673.png[\\/img]毕竟疫苗看起来也快了嘛。[\\/quote]周一看看，这个板块埋伏资金不少，容易坑，咱们得掂量掂量，如果不是高开低走的话，找两个投机倒把的也不是不行";
//		Matcher matcher = IMG_PATTERN.matcher(str);
//		while(matcher.find()) {
//			System.out.println(matcher.group(1));
//		}
//		System.out.println(excludeQuote(str));
		StringBuilder sb = new StringBuilder();
		sb.append("#### 62367楼  \n");
		sb.append("作者:qiaoxuejia\n");
		sb.append("> 引用:感觉快了吧，信号弹已经打出来了，疫情不大规模反弹，院线要复苏了。感觉最近电影相关股票都有可能走一波。\n");
		sb.append("#### 回复:周一看看，这个板块埋伏资金不少，容易坑，咱们得掂量掂量，如果不是高开低走的话，找两个投机倒把的也不是不行\n");
		sb.append("![](http://img.nga.178.com/attachments/mon_201209/14/-47218_5052bc4cc6331.png)\n");
		sb.append("![](http://img.nga.178.com/attachments/mon_201212/24/-1324875_50d841a63a673.png)");
		Map map = new HashMap<>();
		Map markdown = new HashMap<>();
		map.put("msgtype", "markdown");
		map.put("markdown",markdown);
		markdown.put("title", "乔帮主推送");
		markdown.put("text", sb.toString());
		String param = JSONObject.toJSONString(map);
		String rs = Util.postByJson(sendUrl, map);
		System.out.println(rs);
	}
}
