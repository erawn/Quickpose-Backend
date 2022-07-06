void setup(){  
  size(600,600); 
}

void draw(){ 
  background(0,200,0); 
  
  
  //Tweak however you like, whatever the current "render.png" is in main file will be 
  //displayed in the quickpose viewer (nothing else supported at the moment)
  if(frameCount % 60 == 0){
    save("render.png");
  }
}
