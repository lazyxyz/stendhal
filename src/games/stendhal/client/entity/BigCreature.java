/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.entity;

import marauroa.common.game.*;
import games.stendhal.client.*;

import java.awt.*;
import java.awt.geom.*;


/** A Creature entity */
public class BigCreature extends Creature 
  {
  public BigCreature(GameObjects gameObjects, RPObject object) throws AttributeNotFoundException
    {
    super(gameObjects, object);
    }
  
  protected void buildAnimations(RPObject object)
    {
    SpriteStore store=SpriteStore.get();  

    sprites.put("move_up", store.getAnimatedSprite(translate(object.get("class")),0,4,64,64));      
    sprites.put("move_right", store.getAnimatedSprite(translate(object.get("class")),1,4,64,64));      
    sprites.put("move_down", store.getAnimatedSprite(translate(object.get("class")),2,4,64,64));      
    sprites.put("move_left", store.getAnimatedSprite(translate(object.get("class")),3,4,64,64));      

    sprites.get("move_up")[3]=sprites.get("move_up")[1];
    sprites.get("move_right")[3]=sprites.get("move_right")[1];
    sprites.get("move_down")[3]=sprites.get("move_down")[1];
    sprites.get("move_left")[3]=sprites.get("move_left")[1];
    }
  
  protected Sprite defaultAnimation()
    {
    animation="move_up";
    return sprites.get("move_up")[0];
    }

  public Rectangle2D getArea()
    {
    return new Rectangle.Double(x,y,2,2);
    }
    
  public Rectangle2D getDrawedArea()
    {
    return new Rectangle.Double(x,y,2,2);
    }  
    
  }
