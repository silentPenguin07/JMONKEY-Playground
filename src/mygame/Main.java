package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication implements ActionListener {
    
    // instance data
    private static final float FLOOR_LENGTH = 2000, FLOOR_HEIGHT = 0.5f, 
            FLOOR_WIDTH = 2000;
    
    private Node shootables;
    
    private CharacterControl player;
    final private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    
    // temporary vectors used on each frame
    // prevents instantiating new vectors on each frame
    final private Vector3f camDir = new Vector3f();
    final private Vector3f camLeft = new Vector3f();
    
    // set up physics
    BulletAppState bulletAppState = new BulletAppState();
    
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        stateManager.attach(bulletAppState);
        
        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);
        
        // we re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        
        setUpKeys();
        setUpLight();
        makeFloor();
        initCrossHairs();
        
        // collision detection for the player
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(60);
        player.setPhysicsLocation(new Vector3f(0, 0, 0));
        bulletAppState.getPhysicsSpace().add(player);
        
    }
    
    // generates a floor
    private void makeFloor()
    {
        Geometry floor_Geo = new Geometry("Floor", new Box(FLOOR_LENGTH, FLOOR_HEIGHT, 
                FLOOR_WIDTH));
        floor_Geo.setLocalTranslation(0, 0, 5);
        Material floorMat = new Material(assetManager, "MatDefs/Misc/Unshaded.j3md");
        floorMat.setColor("Color", ColorRGBA.DarkGray);
        floor_Geo.setMaterial(floorMat);
        
        shootables.attachChild(floor_Geo);
        
        // make the floor physical with mass 0.0f
        RigidBodyControl floor_Phy = new RigidBodyControl(0.0f);
        floor_Geo.addControl(floor_Phy);
        bulletAppState.getPhysicsSpace().add(floor_Phy);
    }
    
    // generate a box
    private void makeBox(Vector3f pos)
    {
        Geometry boxGeo = new Geometry("Box", new Box(1, 1, 1));
        boxGeo.setLocalTranslation(pos);
        Material boxMat = new Material(assetManager, "MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.Blue);
        boxGeo.setMaterial(boxMat);
        
        shootables.attachChild(boxGeo);
        System.out.println("Made a box at " + pos);
        
        // make the box physical with mass 0.0f
        RigidBodyControl boxPhy = new RigidBodyControl(0.0f);
        boxGeo.addControl(boxPhy);
        bulletAppState.getPhysicsSpace().add(boxPhy);
    }
    
    // remove a box at a certain position
    private void removeBox(CollisionResult box)
    {
        if (shootables.hasChild(box.getGeometry()) 
                && box.getGeometry().getName().equals("Box"))
        {
            shootables.detachChild(box.getGeometry());
            System.out.println("Removed a box at " + box.getGeometry()
                    .getLocalTranslation());
        }
    }
    
    private CollisionResult shoot()
    {
        // list of collision results
        CollisionResults results = new CollisionResults();
        
        // aim the ray from cam loc to cam direction
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        
        // collect the collisions with the shootable items
        shootables.collideWith(ray, results);
        
        // find the closest hit
        if (results.size() > 0)
        {
            CollisionResult closest = results.getClosestCollision();
            return closest;
        }
        return null;
    }
    
    // centered cross to help with aimm
    private void initCrossHairs()
    {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont);
        ch.setSize(2 * guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");
        ch.setLocalTranslation(
            settings.getWidth() / 2 - ch.getLineWidth() / 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }
    
    private void setUpLight()
    {
        // add light to see the objects
        AmbientLight a1 = new AmbientLight();
        a1.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(a1);
        
        DirectionalLight d1 = new DirectionalLight();
        d1.setColor(ColorRGBA.White);
        d1.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(d1);
    }
    
    // overrites the default navigational key mappings to add
    // physics controlled movement
    private void setUpKeys() 
    {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Build", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Break", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Build");
        inputManager.addListener(this, "Break");
    }
    
      /** These are our custom actions triggered by key presses.
    * We do not walk yet, we just keep track of the direction the user pressed. */
    @Override
    public void onAction(String binding, boolean value, float tpf) {
       
        switch (binding) {
            case "Left" -> left= value;
            case "Right" -> right = value;
            case "Up" -> up = (value);
            case "Down" -> down = value;
            case "Jump" -> player.jump();
            default -> {
            }
        }
        if (binding.equals("Build") && !value)
        {
            if (shoot() != null)
            {
                makeBox(shoot().getContactPoint());
            }
        }
        else if (binding.equals("Break") && !value)
        {
            if (shoot() != null)
            {
                removeBox(shoot());
            }
        }
    }

    /**
   * This is the main event loop--walking happens here.
   * We check in which direction the player is walking by interpreting
   * the camera direction forward (camDir) and to the side (camLeft).
   * The setWalkDirection() command is what lets a physics-controlled player walk.
   * We also make sure here that the camera moves with player.
   */
    @Override
    public void simpleUpdate(float tpf) {
        
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        
        if (left)
        {
            walkDirection.addLocal(camLeft);
        }
        if (right)
        {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up)
        {
            walkDirection.addLocal(camDir);
        }
        if (down)
        {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }
    
    
}
