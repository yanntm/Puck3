package com.gildedrose;

public class GenericUpdater implements IItemUpdater {
	@Override
	public void updateQuality(Item item) {
		if (item.quality > 0) {
			item.quality = item.quality - 1;
		}
		item.sellIn = item.sellIn - 1;
		if (item.sellIn < 0) {
			item.quality = item.quality - item.quality;
		}
	}
}
