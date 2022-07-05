void setup(){  

}

void draw(){ 
  
  
  
  //Tweak however you like, whatever the current "render.png" is in main file will be 
  //displayed in the quickpose viewer (nothing else supported at the moment)
  if(frameCount % 60 == 0){
    save("render.png");
  }
}
