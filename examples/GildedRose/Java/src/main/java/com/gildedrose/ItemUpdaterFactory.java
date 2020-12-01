package com.gildedrose;

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
