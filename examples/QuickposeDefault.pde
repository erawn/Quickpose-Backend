void setup(){ 
  size(600,600);
}

void draw(){ 
             
  ellipseMode(CENTER);
  fill(0,255,255);
  background(255);          
  
  float t = map(millis() % 10000,0,10000, 0, PI*2);
  
  arc(height/2, width/2,height,width, t, t+PI,CHORD);
  
  /*Whatever is called "render.png" in your main sketch folder will become the thumbail 
  for that version, you can make it however you want, this is a place to start: */
  if(frameCount % 20 == 0){
    save("render.png"); 
  }
}