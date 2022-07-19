void setup(){  //Setup functions DO NOT update in REPL Mode, 
  size(600,600); //so only put things like screen size in here
}

void draw(){ 
             
  ellipseMode(CENTER);
  fill(0,255,255);
  background(255);          
  
  float t = map(millis() % 10000,0,10000, 0, PI*2);
  
  arc(height/2, width/2,height,width, t, t+PI,CHORD);
  
  
  if(frameCount % 20 == 0){
    save("render.png");
  }
}