package pers.ljy.nga.stock;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import pers.ljy.nga.stock.constant.UrlConstant;
import pers.ljy.nga.stock.main.PullMain;

@Component
public class AutoRunner implements ApplicationRunner{

	
	private static final Pattern IMG_PATTERN = Pattern.compile("\\[img\\](.+)\\[\\/img\\]");
	
	@Value("${nga.stock.tid}")
	String tid;
	@Value("${nga.qiao.stock.tid}")
	String qiao_tid;
	@Value("${dingtalk.url}")
	String ding_url;
	@Value("${dingtalk.url.qiao}")
	String qiao_ding_url;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		// TODO Auto-generated method stub
		PullMain main = new PullMain(tid,ding_url);
		main.init("main-floor");
		PullMain qiaoPull = new PullMain(qiao_tid,qiao_ding_url);
		qiaoPull.init("qiao-floor");
	}

	
}