package engine.game.state;

import org.joml.Vector3f;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class contains all attributes related to the rendering of an object's material.
 * 
 * We do not include any OpenGL bindings in this class. The Texture and Model class in graphics.core
 * are responsible for binding the attributes of this class to our OpenGL shaders to render the material.
 * 
 * There are several benefits to separating a game object's state from the rendering of that object
 * We are able to easily serialize and deserialize this class whole. This allows us to define in-game objects in a 
 * data-driven, declarative way, among other benefits of flexibility.
 * 
 * @author cypress980
 *
 */
public class Material {

    private static final Vector3f DEFAULT_COLOUR = new Vector3f(1.0f, 1.0f, 1.0f);

    private Vector3f color;
    
    private float reflectance;

    private String textureFile;
    
    public Material() {
        color = DEFAULT_COLOUR;
        reflectance = 0;
    }
    
    public Material(Vector3f color, float reflectance) {
        this();
        this.color = color;
        this.reflectance = reflectance;
    }

    public Material(String textureFile, float reflectance) {
        this();
        this.textureFile = textureFile;
        this.reflectance = reflectance;
    }
    
    public Material(String textureFile) {
        this();
        this.textureFile = textureFile;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public float getReflectance() {
        return reflectance;
    }

    public void setReflectance(float reflectance) {
        this.reflectance = reflectance;
    }

    @JsonIgnore
    public boolean isTextured() {
        return this.textureFile != null;
    }

    public String getTextureFile() {
        return textureFile;
    }

    public void setTextureFile(String textureFile) {
        this.textureFile = textureFile;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((color == null) ? 0 : color.hashCode());
	result = prime * result + Float.floatToIntBits(reflectance);
	result = prime * result + ((textureFile == null) ? 0 : textureFile.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Material other = (Material) obj;
	if (color == null) {
	    if (other.color != null)
		return false;
	} else if (!color.equals(other.color))
	    return false;
	if (Float.floatToIntBits(reflectance) != Float.floatToIntBits(other.reflectance))
	    return false;
	if (textureFile == null) {
	    if (other.textureFile != null)
		return false;
	} else if (!textureFile.equals(other.textureFile))
	    return false;
	return true;
    }
}