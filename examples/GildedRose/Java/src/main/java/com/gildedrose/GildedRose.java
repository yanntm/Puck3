package com.gildedrose;

class GildedRose {
    private static final String BACKSTAGE_PASSES_TO_A_TAFKAL80ETC_CONCERT = "Backstage passes to a TAFKAL80ETC concert";
	Item[] items;

    public GildedRose(Item[] items) {
        this.items = items;
    }

    public void updateQuality() {
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
			updateQuality(item);
        }
    }

	private void updateQuality(Item item) {
		if (!item.name.equals("Aged Brie")
		        && !item.name.equals(BACKSTAGE_PASSES_TO_A_TAFKAL80ETC_CONCERT)) {
		    if (item.quality > 0) {
		        if (!item.name.equals("Sulfuras, Hand of Ragnaros")) {
		            item.quality = item.quality - 1;
		        }
		    }
		} else {
		    if (item.quality < 50) {
		        item.quality = item.quality + 1;

		        if (item.name.equals(BACKSTAGE_PASSES_TO_A_TAFKAL80ETC_CONCERT)) {
		            if (item.sellIn < 11) {
		                if (item.quality < 50) {
		                    item.quality = item.quality + 1;
		                }
		            }

		            if (item.sellIn < 6) {
		                if (item.quality < 50) {
		                    item.quality = item.quality + 1;
		                }
		            }
		        }
		    }
		}

		if (!item.name.equals("Sulfuras, Hand of Ragnaros")) {
		    item.sellIn = item.sellIn - 1;
		}

		if (item.sellIn < 0) {
		    if (!item.name.equals("Aged Brie")) {
		        if (!item.name.equals(BACKSTAGE_PASSES_TO_A_TAFKAL80ETC_CONCERT)) {
		            if (item.quality > 0) {
		                if (!item.name.equals("Sulfuras, Hand of Ragnaros")) {
		                    item.quality = item.quality - 1;
		                }
		            }
		        } else {
		            item.quality = item.quality - item.quality;
		        }
		    } else {
		        if (item.quality < 50) {
		            item.quality = item.quality + 1;
		        }
		    }
		}
	}
}