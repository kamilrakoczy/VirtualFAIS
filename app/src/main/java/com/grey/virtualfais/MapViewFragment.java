package com.grey.virtualfais;


import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.grey.virtualfais.models.Level;
import com.grey.virtualfais.models.Room;
import com.grey.virtualfais.services.MapProvider;
import com.qozix.tileview.hotspots.HotSpot;

import java.util.List;

public class MapViewFragment extends TileViewFragment {
    private Level level;
    private MapProvider mapProvider;
    private PathFinder pathFinder = new PathFinder();
    private PathDrawer pathDrawer;
    private Room selectedRoom;

    public void setLevel(Level level){
        if (!this.level.equals(level)) {
            this.level = level;
            mapProvider.setLevel(level);
            getTileView().setSize(level.getPlanWidth(), level.getPlanHeight());
            forceRedraw();
            if(selectedRoom != null) {
                drawPathTo(selectedRoom);
            }
        }
    }

    private void forceRedraw() {
        tileView.setScale(1);
        tileView.setScale(0);
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        super.setArguments(args);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void setupViews() {
        level = (Level) getArguments().get("level");
        mapProvider = new MapProvider(level);

        DetectClick detectClick = new DetectClick (
                getResources(),
                getContext(),
                level
        );

        setupTileView();

        pathDrawer = new PathDrawer(tileView);
        HotSpot hotSpot = new HotSpot();
        hotSpot.set(new Rect(0, 0, level.getPlanWidth(), level.getPlanHeight()));
        hotSpot.setHotSpotTapListener((hotSpot1, x, y) -> handleClick(detectClick, x, y));

        tileView.addHotSpot(hotSpot);
    }

    private void setupTileView() {
        tileView = getTileView();
        // let the image explode
        tileView.setScaleLimits(0, 2);

        // size of original image at 100% mScale
        tileView.setSize(level.getPlanWidth(), level.getPlanHeight());

        // detail levels
        tileView.setBitmapProvider(mapProvider);
        tileView.addDetailLevel(1.000f, "/1000/%d_%d.jpg");
        tileView.addDetailLevel(0.500f, "/500/%d_%d.jpg");
        tileView.addDetailLevel(0.250f, "/250/%d_%d.jpg");
        tileView.addDetailLevel(0.125f, "/125/%d_%d.jpg");

        // set mScale to 0, but keep scaleToFit true, so it'll be as small as possible but still match the container
        tileView.setScale(0);

        // let's use 0-1 positioning...
        tileView.defineBounds(0, 0, 1, 1);

        // frame to center
        frameTo(0.5, 0.5);

        // render while panning
        tileView.setShouldRenderWhilePanning(true);

        // disallow going back to minimum scale while double-taping at maximum scale (for demo purpose)
        tileView.setShouldLoopScale(false);
    }

    public void drawPathTo(Room room){
        selectedRoom = room;
        pathDrawer.clearPath();

        if (room != null) {
            Log.d("HotSpotTapped", "With access through the tag API to the Activity " + room.getId());
            Intent i = new Intent(getActivity(), PopupActivity.class);
            i.putExtra("room_id", room.getId());

            List<Node> path = pathFinder.goToSelectedRoom(room.getId(), level);
            if(!path.isEmpty())
            {
                if (Level.ZERO.equals(level))
                {
                    pathDrawer.newPath(pathFinder.getStartNode().x, pathFinder.getStartNode().y);
                } else {
                    pathDrawer.newPath(path.get(0).x, path.get(0).y);
                    path.remove(0);
                }

                for (Node node : path) {
                    pathDrawer.nextPoint(node.x, node.y);
                }
                pathDrawer.endPath();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleClick(DetectClick detectClick, int x, int y) {
        Log.d("HotSpot", "X/Y " + x + " " + y);
        int scaledX = (int) (x / tileView.getScale());
        int scaledY = (int) (y / tileView.getScale());
        Log.d("HotSpot", "Scaled X/Y " + scaledX + " " + scaledY + " Scale: " + tileView.getScale());
        Room clickedRoom = detectClick.getClosestRoom(scaledX, scaledY, level);
        drawPathTo(clickedRoom);

    }


}
