package com.gildedrose.updater.updaters;

import com.gildedrose.IItemUpdater;
import com.gildedrose.Item;

public class BrieUpdater implements IItemUpdater {
	@Override
	public void updateQuality(Item item) {
		if (item.quality < 50) {
			item.quality = item.quality + 1;
		}


		item.sellIn = item.sellIn - 1;


		if (item.sellIn < 0) {
			if (item.quality < 50) {
				item.quality = item.quality + 1;
			}
		}		
	}

}
