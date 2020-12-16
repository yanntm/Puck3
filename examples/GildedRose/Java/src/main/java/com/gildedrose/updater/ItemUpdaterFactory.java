package com.gildedrose.updater;

import com.gildedrose.GenericUpdater;
import com.gildedrose.IItemUpdater;
import com.gildedrose.Item;
import com.gildedrose.updater.updaters.BackstageUpdater;
import com.gildedrose.updater.updaters.BrieUpdater;
import com.gildedrose.updater.updaters.LegendaryUpdater;

public class ItemUpdaterFactory {
	public static IItemUpdater createUpdater(Item item) {
		if (item.name.equals("Sulfuras, Hand of Ragnaros"))
			return new LegendaryUpdater();
		else if (item.name.equals("Backstage passes to a TAFKAL80ETC concert"))
			return new BackstageUpdater();
		else if (item.name.equals("Aged Brie"))
			return new BrieUpdater();
		else return new GenericUpdater();
	}
}
