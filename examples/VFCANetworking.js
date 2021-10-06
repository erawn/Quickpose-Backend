var request = 'http://127.0.0.1:8080';

const sendFork = async (id) => {
	return await fetch(request + '/fork/' + id)
	.then(function(response){
		var resp = response.json()
		return resp;
	}).then(function(id){
		return id;
	})
	
}
const sendSelect = async (id) => {
	const response = await fetch(request + '/select/' + id);
}

const updatePositionData = async (url, positions) => {
	const response = await fetch(url + '/positions.json', {
		method: "POST",
		body: JSON.stringify(positions)
	}).then(res => {
		//console.log("POST sent, response: ", res);
	});

}
var selectedId = -1
const requestCurrentId = async (url) => {
	const response = await fetch(url+'/currentVersion');
	const id = await response.json();
	if(selectedId !== id){
        return id
	}else{
        return -1
    }
 	
}

var data = null
const requestData = async (url) => {
	const response = await fetch(url+'/versions.json');
	await response.json().then(json => {
        data = json
    });
}

function getIconImage(node){
	return request + "/_" + node.id +"/render.png";
}