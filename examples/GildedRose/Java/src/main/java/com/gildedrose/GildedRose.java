package com.gildedrose;

class GildedRose {
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
		ItemUpdaterFactory.createUpdater(item).updateQuality(item);
	}
}