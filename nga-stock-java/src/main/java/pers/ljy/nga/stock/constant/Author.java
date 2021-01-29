package pers.ljy.nga.stock.constant;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import pers.ljy.nga.stock.main.Properties;
import pers.ljy.nga.stock.main.Properties.AuthorProperties;

@Component
@Import(Properties.class)
public class Author {
	@Autowired
	Properties props;
	public static Map<Integer, String> authors = new HashMap<>();

	@PostConstruct
	public void init() {
		authors = props.getAuthor().stream()
				.collect(Collectors.toMap(AuthorProperties::getUid, AuthorProperties::getName, (x, y) -> y));
	}
//	static {
//		authors.put(27178316, "gxgujnk1993");
//		authors.put(38666451, "泰莫拉尔");
//		authors.put(100921, "barryking");
//		authors.put(42255599, "天之藍～");
//		authors.put(533348, "colaman2006");
//		authors.put(60002731, "qiaoxuejia");
//		authors.put(4627122, "牛中牛神");
//		authors.put(150058, "阿狼");
//		authors.put(5254815, "禾戈禾戈");
//		authors.put(1904077, "白博士acbogeh");
//	}

}
