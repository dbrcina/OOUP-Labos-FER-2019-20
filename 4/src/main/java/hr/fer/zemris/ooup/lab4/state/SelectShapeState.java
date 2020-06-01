package hr.fer.zemris.ooup.lab4.state;

import hr.fer.zemris.ooup.lab4.model.DocumentModel;
import hr.fer.zemris.ooup.lab4.model.GraphicalObject;
import hr.fer.zemris.ooup.lab4.renderer.Renderer;
import hr.fer.zemris.ooup.lab4.util.Point;

public class SelectShapeState implements State {

    private final DocumentModel model;

    public SelectShapeState(DocumentModel model) {
        this.model = model;
    }

    @Override
    public void mouseDown(Point mousePoint, boolean shiftDown, boolean ctrlDown) {

    }

    @Override
    public void mouseUp(Point mousePoint, boolean shiftDown, boolean ctrlDown) {

    }

    @Override
    public void mouseDragged(Point mousePoint) {

    }

    @Override
    public void keyPressed(int keyCode) {

    }

    @Override
    public void afterDraw(Renderer r, GraphicalObject go) {

    }

    @Override
    public void afterDraw(Renderer r) {

    }

    @Override
    public void onLeaving() {

    }

}
