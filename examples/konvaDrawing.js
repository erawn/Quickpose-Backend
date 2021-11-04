var stage
var layer
var tr

var selectedColor
var nonSelectedColor
var PADDING = 500;
var scaleBy = 1.05;
var nodeRadius = 30;
function konvaInit() {
    
    stage = new Konva.Stage({
        container: 'container',
        width: window.innerWidth,
        height: window.innerHeight,
        draggable: true,
      });

    layer = new Konva.Layer();
    stage.add(layer);

    stage.on('wheel', (e) => {
        e.evt.preventDefault();
        var oldScale = stage.scaleX();

        var pointer = stage.getPointerPosition();

        var mousePointTo = {
          x: (pointer.x - stage.x()) / oldScale,
          y: (pointer.y - stage.y()) / oldScale,
        };

        var newScale =
          e.evt.deltaY > 0 ? oldScale * scaleBy : oldScale / scaleBy;

        stage.scale({ x: newScale, y: newScale });

        var newPos = {
            x: pointer.x - mousePointTo.x * newScale,
            y: pointer.y - mousePointTo.y * newScale,
        };
        stage.position(newPos);
    });

    tr = new Konva.Transformer({
        keepRatio: false,
    });
    layer.add(tr)

    stage.on('dblclick dbltap', function(e) {
        e.evt.preventDefault();
        if(e.target === stage){ //if click on empty area
            var floatingText = newFloatingText(stage.getPointerPosition())
            tr.nodes([floatingText]);
            floatingText.draggable(true)
            floatingText.fire('dblclick')
        }
        
    })
    //https://konvajs.org/docs/select_and_transform/Basic_demo.html
    

    var transformable = ['floatingText']

    stage.on('click tap', function (e) {
        // if click on empty area - remove all selections
        if (e.target === stage) {
            tr.nodes([]);
            transformable.forEach( function (name){
                stage.find('.' + name).forEach( (element) =>{
                    element.draggable(false)
                })
            })
            return;
          }
  
          // do nothing if clicked NOT on our rectangles
          if (!e.target.hasName('floatingText')) {
            return;
          }

          const metaPressed = e.evt.shiftKey || e.evt.ctrlKey || e.evt.metaKey;
          const isSelected = tr.nodes().indexOf(e.target) >= 0;
  
          if (!metaPressed && !isSelected) {
            // if no key pressed and the node is not selected
            // select just one
            tr.nodes([e.target]);
            e.target.draggable(true)
          } else if (metaPressed && isSelected) {
            // if we pressed keys and node was selected
            // we need to remove it from selection:
            const nodes = tr.nodes().slice(); // use slice to have new copy of array
            // remove node from array
            nodes.splice(nodes.indexOf(e.target), 1);
            tr.nodes(nodes);
            e.target.draggable(false)
          } else if (metaPressed && !isSelected) {
            // add the node into selection
            const nodes = tr.nodes().concat([e.target]);
            tr.nodes(nodes);
            e.target.draggable(true)
          }
    });

    selectedColor = Konva.Util.getRGB('rgb(0,255,0)');
    nonSelectedColor = Konva.Util.getRGB('rgb(255,0,0)');
}

//https://konvajs.org/docs/select_and_transform/Basic_demo.html

// function konvaDrawNodes(nodes) {
//     for (i = 0; i < nodes.length; i++) {
//         node = nodes[i]
//         console.log(n.id)
//         const circle = newCircle(node);
//         const label = new
//         layer.add(circle);
//     }
// }

function konvaUpdate(nodes, links) {
    for (i = 0; i < nodes.length; i++) {
        var node = nodes[i]
        var circle = layer.findOne('.node-' + node.id);
        var label = layer.findOne('.label-' + node.id);
        if (typeof circle == 'undefined') {
            circle = newCircle(node);
        }
        if (typeof label == 'undefined') {
            label = newText(node);
        }
        
        //console.log(circle)
        circle.x(node.x);
        circle.y(node.y);
        label.x(node.x + nodeRadius/2);
        label.y(node.y + nodeRadius);
    };
    for (i = 0; i < links.length; i++) {
        var link = links[i];
        var line = layer.findOne('.edge-' + link.index);
        if (typeof line == 'undefined') {
            line = newLine(link);
        }
        line.moveToBottom()
        line.points([link.target.x, link.target.y,
        link.source.x, link.source.y]);
    };
    updateSelectedNode()
}
function newText(node) {
    var textNode = new Konva.Text({
        x: 10,
        y: 15,
        text: 'Node ' + node.id,
        name: 'label-' + node.id,
        fontSize: 30,
        fontFamily: 'Calibri',
        fill: 'green'
      });
      layer.add(textNode)

      //https://konvajs.org/docs/sandbox/Editable_Text.html
      textNode.on('dblclick dbltap', () => {

        var textPosition = textNode.getAbsolutePosition();
        var stageBox = stage.container().getBoundingClientRect();

        var areaPosition = {
          x: stageBox.left + textPosition.x,
          y: stageBox.top + textPosition.y,
        };

        var textarea = document.createElement('textarea');
        document.body.appendChild(textarea);

        textarea.value = textNode.text();
        textarea.style.position = 'absolute';
        textarea.style.top = areaPosition.y + 'px';
        textarea.style.left = areaPosition.x + 'px';
        textarea.style.width = textNode.width();

        textarea.focus();

        function removeTextarea() {
                document.body.removeChild(textarea);
                window.removeEventListener('mousedown', handleOutsideClick);
              }

        textarea.addEventListener('keydown', function (e) {
          // hide on enter
          console.log(e.key)
          if (e.key === 'Enter') {
            textNode.text(textarea.value);
            removeTextarea()
          }
          if (e.keyCode === 27) {
            removeTextarea()
          }
        });
        function handleOutsideClick(e) {
            if (e.target !== textarea) {
              textNode.text(textarea.value);
              removeTextarea()
            }
          }
          setTimeout(() => {
            window.addEventListener('mousedown', handleOutsideClick);
          });
      });
      return textNode
}
function newCircle(node) {
    var circle = new Konva.Circle({
        radius: nodeRadius,
        name: 'node-' + node.id,
        draggable: true,
        id: node.id,
        fillPatternRepeat: 'no-repeat',
        stroke: 'blue',
        strokeWidth: 4
    });
    circle.on('dragmove', () => {
        node.x = circle.x(),
            node.y = circle.y(),
            node.fx = circle.x(),
            node.fy = circle.y();
    })
    circle.on('dragmove', () => {
        //force.resume();
    })
    circle.on('click', () => {
        selectNode(node),
        updateSelectedNode();
    })
    circle.on('dblclick dbltap', () => {
        doubleClicked(node),
        updateSelectedNode();
    })
    loadIconImage(circle)
    layer.add(circle)
    return circle;
}



function updateSelectedNode() {
    var list = layer.find('Circle')
    list.forEach(circle => {
        if (circle.id() == selectedId) {
            circle.stroke('green')
        } else {
            circle.stroke('blue')
        }
    })
}
function newLine(link) {
    var line = new Konva.Line({
        points: [],
        stroke: 'black',
        name: 'edge-' + link.index
    });
    layer.add(line);
    return line;
}

function konvaDrawLayer() {
    layer.draw()
}

function updateSelectedIconImage(){
    if(selectedId >= 0){
        var circle = layer.findOne('.node-' + selectedId);
        if (typeof circle !== 'undefined'){
            loadIconImage(circle);
        }
        
    }
}

function loadIconImage(circle) {
    var imageObj = new Image();
    imageObj.onload = function () {
        circle.fillPatternImage(imageObj);
        circle.fillPatternScale({ x: circle.radius() * 2 / imageObj.width, y: circle.radius() * 2 / imageObj.height })
        circle.fillPatternOffset({ x: circle.width() * 5, y: circle.width() * 5 })
    }
    imageObj.src = getIconImageURL(circle.id());
}

//https://konvajs.org/docs/sandbox/Editable_Text.html
function newFloatingText(pos){
    var textNode = new Konva.Text({
        text: 'Some text here',
        x: pos.x,
        y: pos.y,
        fontSize: 20,
        draggable: false,
        width: 200,
        name: 'floatingText',
      });

      layer.add(textNode);

    //   var tr = new Konva.Transformer({
    //     node: textNode,
    //     enabledAnchors: [
    //         'top-left',
    //         'top-right',
    //         'bottom-left',
    //         'bottom-right',
    //       ],
    //     // set minimum width of text
    //     boundBoxFunc: function (oldBox, newBox) {
    //       newBox.width = Math.max(30, newBox.width);
    //       return newBox;
    //     },
    //   });

      textNode.on('transform', function () {
        // reset scale, so only with is changing by transformer
        textNode.setAttrs({
          width: textNode.width() * textNode.scaleX(),
          scaleX: 1,
          height: textNode.height() * textNode.scaleY(),
          scaleY: 1,
        });
      });

    //   layer.add(tr);

    //   tr.hide()
      //'mousedown touchstart'
    //   textNode.on('mousedown touchstart', function(e) {
    //     e.evt.preventDefault();
    //     tr.show();
    //     textNode.draggable(true);
    //     function handleOutsideClickTransform(e) {
    //         console.log(e.target)
    //         if (e.target !== textNode || e.target !== tr) {
    //           tr.hide();
    //           textNode.draggable(false)
    //           window.removeEventListener('mousedown', handleOutsideClickTransform);
    //         }
    //       }
    //       setTimeout(() => {
    //         window.addEventListener('mousedown', handleOutsideClickTransform);
    //       });
    //   });

      textNode.on('dblclick dbltap', function(e) { //TODO - prevent double clicks from going through if first click is on different element
        // hide text node and transformer:
        textNode.hide();
        //tr.hide();
        // create textarea over canvas with absolute position
        // first we need to find position for textarea
        // how to find it?

        // at first lets find position of text node relative to the stage:
        var textPosition = textNode.absolutePosition();

        // so position of textarea will be the sum of positions above:
        var areaPosition = {
          x: stage.container().offsetLeft + textPosition.x,
          y: stage.container().offsetTop + textPosition.y,
        };

        // create textarea and style it
        var textarea = document.createElement('textarea');
        document.body.appendChild(textarea);

        // apply many styles to match text on canvas as close as possible
        // remember that text rendering on canvas and on the textarea can be different
        // and sometimes it is hard to make it 100% the same. But we will try...
        textarea.value = textNode.text();
        textarea.style.position = 'absolute';
        textarea.style.top = areaPosition.y + 'px';
        textarea.style.left = areaPosition.x + 'px';
        textarea.style.width = textNode.width() - textNode.padding() * 2 + 'px';
        textarea.style.height =
          textNode.height() - textNode.padding() * 2 + 5 + 'px';
        textarea.style.fontSize = textNode.fontSize() + 'px';
        textarea.style.border = 'none';
        textarea.style.padding = '0px';
        textarea.style.margin = '0px';
        textarea.style.overflow = 'hidden';
        textarea.style.background = 'none';
        textarea.style.outline = 'none';
        textarea.style.resize = 'none';
        textarea.style.lineHeight = textNode.lineHeight();
        textarea.style.fontFamily = textNode.fontFamily();
        textarea.style.transformOrigin = 'left top';
        textarea.style.textAlign = textNode.align();
        textarea.style.color = textNode.fill();
        rotation = textNode.rotation();
        var transform = '';
        if (rotation) {
          transform += 'rotateZ(' + rotation + 'deg)';
        }

        var px = 0;
        // also we need to slightly move textarea on firefox
        // because it jumps a bit
        var isFirefox =
          navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
        if (isFirefox) {
          px += 2 + Math.round(textNode.fontSize() / 20);
        }
        transform += 'translateY(-' + px + 'px)';

        textarea.style.transform = transform;

        // reset height
        textarea.style.height = 'auto';
        // after browsers resized it we can set actual value
        textarea.style.height = textarea.scrollHeight + 3 + 'px';

        textarea.focus();

        function removeTextarea() {
          textarea.parentNode.removeChild(textarea);
          window.removeEventListener('mousedown', handleOutsideClick);
          textNode.show();
          //tr.show();
          //tr.forceUpdate();
        }

        function setTextareaWidth(newWidth) {
          if (!newWidth) {
            // set width for placeholder
            newWidth = textNode.placeholder.length * textNode.fontSize();
          }
          // some extra fixes on different browsers
          var isSafari = /^((?!chrome|android).)*safari/i.test(
            navigator.userAgent
          );
          var isFirefox =
            navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
          if (isSafari || isFirefox) {
            newWidth = Math.ceil(newWidth);
          }

          var isEdge =
            document.documentMode || /Edge/.test(navigator.userAgent);
          if (isEdge) {
            newWidth += 1;
          }
          textarea.style.width = newWidth + 'px';
        }

        textarea.addEventListener('keydown', function (e) {
          // hide on enter
          // but don't hide on shift + enter
          if (e.keyCode === 13 && !e.shiftKey) {
            textNode.text(textarea.value);
            removeTextarea();
          }
          // on esc do not set value back to node
          if (e.keyCode === 27) {
            removeTextarea();
          }
        });

        textarea.addEventListener('keydown', function (e) {
          scale = textNode.getAbsoluteScale().x;
          setTextareaWidth(textNode.width() * scale);
          textarea.style.height = 'auto';
          textarea.style.height =
            textarea.scrollHeight + textNode.fontSize() + 'px';
        });

        function handleOutsideClick(e) {
          if (e.target !== textarea) {
            textNode.text(textarea.value);
            removeTextarea();
          }
        }
        setTimeout(() => {
          window.addEventListener('mousedown', handleOutsideClick);
        });
      });

      return textNode
}