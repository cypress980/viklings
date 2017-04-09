package graphics.flat;

import engine.game.state.Position;
import graphics.core.Model;

public class Text implements FlatRenderable {

    private String text;
    
    private Font font;
    
    private Model model;
    
    private Position position;
    
    public Text(String text) throws Exception {
        this.text = text;
        this.font = new Font();
        this.model = font.buildMesh(text);
        this.position = new Position();
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) throws Exception {
        this.text = text;
        model.deleteBuffers();
        model = font.buildMesh(text);
    }
    
    public void setColor(int r, int g, int b) throws Exception {
	font.setColor(r, g, b);
	model.deleteBuffers();
	model = font.buildMesh(text);
    }
    
    public void setPosition(float x, float y) {
        this.position.setCoordinates(x, y, 0);
    }
    
    public Model getModel() {
	return model;
    }
    
    public void setFont(Font font) throws Exception {
	this.font = font;
	model.deleteBuffers();
	model = font.buildMesh(text);
    }

    @Override
    public Position getPosition() {
	return position;
    }

    @Override
    public void setZOrder(float zOrder) {
	this.position.setCoordinates(position.getCoordinates().setComponent(2, zOrder));
    }
}