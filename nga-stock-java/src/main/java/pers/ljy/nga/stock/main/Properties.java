package pers.ljy.nga.stock.main;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nga")
public class Properties {

	private List<StockProperties> stock;

	public static class StockProperties {
		private String tid;
		private String name;

		public String getTid() {
			return tid;
		}

		public void setTid(String tid) {
			this.tid = tid;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public List<StockProperties> getStock() {
		return stock;
	}

	public void setStock(List<StockProperties> stock) {
		this.stock = stock;
	}
	
	
}
