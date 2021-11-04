var stage
var layer

var selectedColor
var nonSelectedColor
var PADDING = 500;
var scaleBy = 1.05;

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

    selectedColor = Konva.Util.getRGB('rgb(0,255,0)');
    nonSelectedColor = Konva.Util.getRGB('rgb(255,0,0)');
}

// function konvaDrawNodes(nodes) {
//     for (i = 0; i < nodes.length; i++) {
//         node = nodes[i]
//         console.log(n.id)
//         const circle = newCircle(node);
//         const label = new
//         layer.add(circle);
//     }
// }

var labelOffsetX = 2
var labelOffsetY = 5
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
        label.x(node.x + labelOffsetX);
        label.y(node.y + labelOffsetY);
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

        textarea.addEventListener('keydown', function (e) {
          // hide on enter
          console.log(e.key)
          if (e.key === 'Enter') {
            textNode.text(textarea.value);
            document.body.removeChild(textarea);
          }
        });
      });
      return textNode
}
function newCircle(node) {
    var circle = new Konva.Circle({
        radius: 30,
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

// var konvaObject = new Konva.Circle({
//     x: 100,
//     y: 100,
//     radius: 300,
//     stroke: this.color,
//     strokeWidth: 6,
//     fillPatternRepeat: 'no-repeat',
// });

// // load the image into the shape:
// var imageObj = new Image();
// imageObj.onload = function () {
//     konvaObject.fillPatternImage(imageObj);
//     konvaObject.draw();
// }
// imageObj.src = 'www.demo.com/anImageName.png';

// dataset.edges.forEach((edge, i) => {
//     const line = new Konva.Line({
//       points: [],
//       stroke: 'black',
//       name: 'edge-' + i
//     });
//     layer.add(line);
//   })

//   dataset.nodes.forEach((n, i) => {
//     const circle = new Konva.Circle({
//       radius: 30,
//       fill: Konva.Util.getRandomColor(),
//       name: 'node-' + i,
//       draggable: true
//     });
//     circle.on('dragmove', () => {
//       n.x = circle.x(),
//       n.y = circle.y();
//     })
//     circle.on('dragmove', () => {
//       force.resume();
//     })
//     layer.add(circle);
//   })



//   force.on("tick", function(){
//     dataset.nodes.forEach((node, i) => {
//       const circle = layer.findOne('.node-' + i);
//       circle.x(node.x);
//       circle.y(node.y);
//     });
//     dataset.edges.forEach((edge, i) => {
//       const { target, source } = edge;
//       const line = layer.findOne('.edge-' + i);
//       line.points([target.x, target.y, source.x, source.y]);
//     });
//     layer.draw();
//   });