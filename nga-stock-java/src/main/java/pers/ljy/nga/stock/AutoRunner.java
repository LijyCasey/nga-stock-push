package pers.ljy.nga.stock;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import pers.ljy.nga.stock.main.Properties;
import pers.ljy.nga.stock.main.PullFactory;
import pers.ljy.nga.stock.main.Properties.StockProperties;

@Component
@Import(Properties.class)
public class AutoRunner implements ApplicationRunner {

	@Autowired
	Properties props;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		List<StockProperties> stocks = props.getStock();
		stocks.forEach(s -> {
			PullFactory.initPull(s.getTid(), s.getName());
		});
	}

}
