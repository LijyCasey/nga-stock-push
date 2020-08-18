package pers.ljy.nga.stock.constant;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlConstant {

	public static String METHOD = "post";

	public static Map<String, String> HEADERS = new HashMap<>();

	public static Map<String, String> DINGDING_HEADERS = new HashMap<>();

	static {
		HEADERS.put("Content-Type", "application/x-www-form-urlencoded");
		HEADERS.put("X-User-Agent", "NGA_skull/6.0.7(iPhone11,6;iOS 12.2)");
		HEADERS.put("User-Agent", "NGA/6.0.7 (iPhone; iOS 12.2; Scale/3.00)");
		HEADERS.put("Accept-Language", "zh-Hans-CN;q=1");
		DINGDING_HEADERS.put("Content-Type", "application/json");
	}

	public static String URL;

	@Value("${nga.api.url}")
	public void setUrl(String url) {
		UrlConstant.URL = url;
	}
}
