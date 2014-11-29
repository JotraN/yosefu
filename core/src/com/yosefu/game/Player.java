package com.yosefu.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Player extends Rectangle {
    private final Yosefu game;
    private final Engine engine;
    private float velX = 0, velY = 0;
    private final float SPEED = 6;
    private Texture spriteSheet;
    private TextureRegion currentFrame;
    private final Animation STANDING;
    private final Animation LEFTRIGHT;
    private final Animation UP;
    private final Animation DOWN;
    private float stateTime;
    private boolean checkInteraction = false;
    private boolean interacting = false;
    private Location playerLocation;
    private Stats stats;

    public Player(Yosefu game, Engine engine) {
        this.game = game;
        this.engine = engine;
        x = 320;
        y = 64;
        width = 46;
        height = 62;

        spriteSheet = new Texture(Gdx.files.internal("playersheet.png"));
        int rows = 4, columns = 4;
        TextureRegion[][] frames = TextureRegion.split(spriteSheet, spriteSheet.getWidth() / columns, spriteSheet.getHeight() / rows);
        LEFTRIGHT = new Animation(0.1f, frames[0]);
        STANDING = new Animation(0.1f, frames[1]);
        DOWN = new Animation(0.1f, frames[2]);
        UP = new Animation(0.1f, frames[3]);
        stateTime = 0;
        currentFrame = UP.getKeyFrame(0);
        playerLocation = new Location(this, 64);

//        stats = new Stats(10, 10, 0.123f);
        stats = new Stats(10, 10, 2.123f);
    }

    public void draw(Yosefu game) {
        game.batch.enableBlending();
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.batch.draw(currentFrame, x, y);
    }

    public void processEvents() {
        if (interacting) return;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))
            velX = -SPEED;
        else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            velX = SPEED;
        else velX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))
            velY = SPEED;
        else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed((Input.Keys.DOWN)))
            velY = -SPEED;
        else velY = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.O))
            checkInteraction = true;
    }

    public void update(Level level) {
        updateTextureRegion();
        moveAndCollide(level);
        interact(level);
    }

    private void updateTextureRegion() {
        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion original;
        if(currentFrame != null)
            original = currentFrame;
        else
            original = DOWN.getKeyFrame(0);
        currentFrame = LEFTRIGHT.getKeyFrame(stateTime, true);
        if (velY > 0) currentFrame = UP.getKeyFrame(stateTime, true);
        else if (velY < 0) currentFrame = DOWN.getKeyFrame(stateTime, true);
        else if (velX > 0) {
            if (currentFrame.isFlipX())
                currentFrame.flip(true, false);
        } else if (velX < 0) {
            if (!currentFrame.isFlipX())
                currentFrame.flip(true, false);
        } else currentFrame = original;
    }

    public void dispose() {
        spriteSheet.dispose();
    }

    private void moveAndCollide(Level level) {
        x += velX;
        String[][] tileMap = level.getTileMap();
        int blockWidth = 64;
        int mapWidth = tileMap[0].length * blockWidth;
        if (x < 0 || x > mapWidth - width)
            x -= velX;
        playerLocation.update(this);
        int col = playerLocation.right;
        if (velX < 0) col = playerLocation.left;
        int rowBottom = playerLocation.bottom;
        int rowTop = playerLocation.top;
        if (!tileMap[rowBottom][col].equals(Level.EMPTY) || !tileMap[rowTop][col].equals(Level.EMPTY)) {
            if (tileMap[rowBottom][col].equals(Level.WALL) || tileMap[rowTop][col].equals(Level.WALL))
                x -= velX;
            else if (Level.NPC.contains(tileMap[rowBottom][col]) || Level.NPC.contains(tileMap[rowTop][col]))
                x -= velX;
            else if (Level.ENEMY.contains(tileMap[rowBottom][col]) || Level.ENEMY.contains(tileMap[rowTop][col])) {
                if (level.getEnemies().first().isAlive())
                    game.setScreen(new TransitionScreen(game, new Battle(this, level.getEnemies().first(), game, engine)));
            }
            else if (Level.DOOR.contains(tileMap[rowBottom][col]) || Level.DOOR.contains(tileMap[rowTop][col])){
                String nextLevel = level.getObjectVariables().get(tileMap[rowBottom][col]);
                if(nextLevel == null)
                    nextLevel = level.getObjectVariables().get(tileMap[rowTop][col]);
                if(nextLevel == null) return;
                String[] doorDetails = nextLevel.split("\\s");
                x = Integer.parseInt(doorDetails[1]);
                y = Integer.parseInt(doorDetails[2]);
                level.changeLevel(doorDetails[0]);
            }
        }

        y += velY;
        playerLocation.update(this);
        int row = playerLocation.top;
        if (velY < 0) row = playerLocation.bottom;
        int leftCol = playerLocation.left;
        int rightCol = playerLocation.right;
        if (!tileMap[row][leftCol].equals(Level.EMPTY) || !tileMap[row][rightCol].equals(Level.EMPTY)) {
            if (tileMap[row][leftCol].equals(Level.WALL) || tileMap[row][rightCol].equals(Level.WALL))
                y -= velY;
            else if (Level.NPC.contains(tileMap[row][leftCol]) || Level.NPC.contains(tileMap[row][rightCol]))
                y -= velY;
            else if (Level.ENEMY.contains(tileMap[row][leftCol]) || Level.ENEMY.contains(tileMap[row][rightCol])) {
                if (level.getEnemies().first().isAlive())
                    game.setScreen(new TransitionScreen(game, new Battle(this, level.getEnemies().first(), game, engine)));
            }
            else if (Level.DOOR.contains(tileMap[row][leftCol]) || Level.DOOR.contains(tileMap[row][rightCol])){
                String nextLevel = level.getObjectVariables().get(tileMap[row][leftCol]);
                if(nextLevel == null)
                    nextLevel = level.getObjectVariables().get(tileMap[row][rightCol]);
                if(nextLevel == null) return;
                String[] doorDetails = nextLevel.split("\\s");
                x = Integer.parseInt(doorDetails[1]);
                y = Integer.parseInt(doorDetails[2]);
                level.changeLevel(doorDetails[0]);
            }
        }
    }

    private void interact(Level level) {
        playerLocation.update(this);
        // TODO Interact with every direction.
        if (checkInteraction || interacting) {
            Array<NPC> tmp = level.getNpcs();
            int blockWidth = 64;
            for (NPC npc : tmp) {
                int npcRow = (int) Math.floor(npc.y / blockWidth);
                int npcCol = (int) Math.floor(npc.x / blockWidth);
                if (playerLocation.bottom == npcRow - 1 && (playerLocation.left == npcCol || playerLocation.right == npcCol)){
                    if (interacting) {
                        // Update interaction status.
                        interacting = npc.talking();
                    } else {
                        // Start interacting.
                        velX = 0;
                        velY = 0;
                        npc.talk();
                        interacting = npc.talking();
                    }
                    break;
                }
            }
            checkInteraction = false;
        }
    }

    public Stats getStats(){
        return stats;
    }

    public TextureRegion getTexture(){
        return LEFTRIGHT.getKeyFrame(0);
    }
}
