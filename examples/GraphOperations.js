
function resetData(nodes, baseNodes) {
    var nodeIds = nodes.map(function (node) { return node.id })
    baseNodes.forEach(function (node) {
        if (nodeIds.indexOf(node.id) === -1) {
            nodes.push(node)
        }
    })
    return nodes
}

function updateNodes(input_nodes, nodes) {
    var newIndicies = [];
    var oldIndicies = [];
    for (i = 0; i < input_nodes.length; i++) {
        newIndicies.push(input_nodes[i].id);
    }
    for (i = 0; i < nodes.length; i++) {
        oldIndicies.push(nodes[i].id);

    }
    for (i = 0; i < nodes.length; i++) {
        if (!newIndicies.includes(nodes[i].id)) {
            nodes.splice(i, 1);
            changed = true;
        }
    }

    for (i = 0; i < input_nodes.length; i++) {
        if (!oldIndicies.includes(input_nodes[i].id)) {
            baseNodes.push(input_nodes[i]); //TODO: fix this???
            changed = true;
        }
    }
    return nodes;
}

function updateLinks(links, baseNodes) {

    var linksCopy = links
    var nodesCopy = [];
    for (i = 0; i < baseNodes.length; i++) {
        nodesCopy.push(baseNodes[i].id);
    }

    for (i = 0; i < links.length; i++) {

        var source = linksCopy[i].source;
        var target = linksCopy[i].target;

        var sourceInd = nodesCopy.indexOf(source);
        var targetInd = nodesCopy.indexOf(target);

        if (sourceInd == -1) {
            sourceInd = 0;
        }
        if (targetInd == -1) {
            targetInd = 0;
        }
        links[i].source = baseNodes[sourceInd];
        links[i].target = baseNodes[targetInd];
    }

    return links
}
function selectNode(selectedNode) {
    selectedId = selectedNode.id
    sendSelect(selectedNode.id);
}




function restorePositions(){


}