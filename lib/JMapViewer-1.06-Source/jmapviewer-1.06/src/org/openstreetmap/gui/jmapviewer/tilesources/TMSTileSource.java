// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.tilesources;

public class TMSTileSource extends AbstractTMSTileSource {

    protected int maxZoom;
    protected int minZoom = 0;

    public TMSTileSource(String name, String url, String id, int maxZoom) {
        super(name, url, id);
        this.maxZoom = maxZoom;
    }

    public TMSTileSource(String name, String url, String id, int minZoom, int maxZoom) {
        super(name, url, id);
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    @Override
    public int getMinZoom() {
        return (minZoom == 0) ? super.getMinZoom() : minZoom;
    }

    @Override
    public int getMaxZoom() {
        return (maxZoom == 0) ? super.getMaxZoom() : maxZoom;
    }

    @Override
    public TileUpdate getTileUpdate() {
        return TileUpdate.IfNoneMatch;
    }
}
