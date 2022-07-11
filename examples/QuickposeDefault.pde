void setup(){  
  size(600,600); 
  ellipseMode(CENTER);
  fill(0,255,255,60);
}

void draw(){ 
             
   
  background(255);          
  
  float t = map(millis() % 10000,0,10000, 0, PI*2);
  
  arc(height/2, width/2,height,width, t, t+PI,CHORD);
  
  
  if(frameCount % 60 == 0){
    save("render.png");
  }
}