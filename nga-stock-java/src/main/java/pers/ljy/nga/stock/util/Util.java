package pers.ljy.nga.stock.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import net.minidev.json.JSONObject;
import pers.ljy.nga.stock.constant.UrlConstant;

public class Util {

	public static String post(String url,Map<String,String> headers,Map<String,String> param) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(url);
		headers.forEach((k,v)->{
			post.setHeader(k, v);
		});
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		Iterator<Map.Entry<String, String>> it = new TreeMap(param).entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> me = it.next();
			nvps.add(new BasicNameValuePair(me.getKey(), me.getValue()));
		}
		post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		HttpResponse response = httpclient.execute(post);
		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity);
		httpclient.close();
		return body;
	}
	
	public static String postByJson(String url,Map param) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(url);
		UrlConstant.DINGDING_HEADERS.forEach((k,v)->{
			post.setHeader(k, v);
		});
		String jsonStr = JSONObject.toJSONString(param);
		post.setEntity(new StringEntity(jsonStr, "UTF-8"));
		HttpResponse response = httpclient.execute(post);
		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity);
		httpclient.close();
		return body;
	}
	
	public static String postByJson(String url,JSONObject param) throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(url);
		UrlConstant.DINGDING_HEADERS.forEach((k,v)->{
			post.setHeader(k, v);
		});
		String jsonStr = param.toJSONString();
		post.setEntity(new StringEntity(jsonStr, "UTF-8"));
		HttpResponse response = httpclient.execute(post);
		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity);
		httpclient.close();
		return body;
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		Map<String, String> param = new HashMap<>();
		param.put("tid", "24913158");
		param.put("page", "826");
		String response = post("http://ngabbs.com/app_api.php?__lib=post&__act=list",UrlConstant.HEADERS,param);
		System.out.println(response);
	}
}
