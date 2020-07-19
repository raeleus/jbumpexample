/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2020 Raymond Buckley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.ray3k.jbumpexample.platformer;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.dongbat.jbump.Item;

import static com.ray3k.jbumpexample.platformer.Core.spriteBatch;

public abstract class Entity {
    public Animation<AtlasRegion> animation;
    public float animationTime;
    public float x;
    public float y;
    public float bboxX;
    public float bboxY;
    public float bboxWidth;
    public float bboxHeight;
    public float rotation;
    public float deltaX;
    public float deltaY;
    public boolean flipX;
    public boolean flipY;
    public float gravityX;
    public float gravityY;
    public Item<Entity> item;
    
    public abstract void act(float delta);
    
    public void draw() {
        if (animation != null) {
            AtlasRegion region = animation.getKeyFrame(animationTime);
            spriteBatch.draw(region, x, y, region.getRegionWidth() / 2f, region.getRegionHeight() / 2f, region.getRegionWidth(), region.getRegionHeight(), flipX ? -1 : 1, flipY ? -1 : 1, rotation);
        }
    }
}
