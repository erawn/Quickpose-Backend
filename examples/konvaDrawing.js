var stage
var layer
function konvaInit() {
    stage = new Konva.Stage({
        container: 'container',
        width: window.innerWidth,
        height: window.innerHeight
    });

    layer = new Konva.Layer();
    stage.add(layer);

    const circle1 = new Konva.Circle({
        radius: 40,
        fill: Konva.Util.getRandomColor(),
        draggable: true
    });
    layer.add(circle1)

}

function konvaDrawNodes(nodes) {
    for (i = 0; i < nodes.length; i++) {
        node = nodes[i]
        console.log(n.id)
        const circle = newCircle(node);

        layer.add(circle);
    }
}

function konvaUpdate(nodes, links) {
    for (i = 0; i < nodes.length; i++) {
        var node = nodes[i]
        var circle = layer.findOne('.node-' + node.id);
        if (typeof circle == 'undefined') {
            circle = newCircle(node);
        }
        //console.log(circle)
        circle.x(node.x);
        circle.y(node.y);
    };
    for (i = 0; i < links.length; i++) {
        var link = links[i];
        var line = layer.findOne('.edge-' + link.index);
        if (typeof line == 'undefined') {
            line = newLine(link);
        }
        line.points([link.target.x, link.target.y, 
           link.source.x, link.source.y]);
    };
}

function newCircle(node) {
    var circle = new Konva.Circle({
        radius: 30,
        fill: Konva.Util.getRandomColor(),
        name: 'node-' + node.id,
        draggable: true
    });
    circle.on('dragmove', () => {
      node.x = circle.x(),
      node.y = circle.y();
    })
    circle.on('dragmove', () => {
      //force.resume();
    })
    layer.add(circle)
    return circle;
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