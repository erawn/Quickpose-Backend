var request = 'http://127.0.0.1:8080';

const sendFork = async (id) => {
	const response = await fetch(request + '/fork/' + id)
	return await response.json();
	
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
const requestCurrentId = async (url) => {
	const response = await fetch(url+'/currentVersion');
	const id = await response.json();
	if(selectedId !== id){
        return id
	}else{
        return -1
    }
 	
}

var oldData = null
var data = null
const requestData = async (url) => {
	const response = await fetch(url+'/versions.json');
	await response.json().then(json => {
		oldData = _.cloneDeep(data)
		data = json
    });
}

function getIconImageURL(id){
	return request + "/image/" + id + "?" + ((new Date()).getTime()); //Add Time to avoid Caching so images update properly
	//const response = fetch(request + "/image/" + node.id);
	// if(response.status== 200){
	// 	return response.url;
	// }
	// return null
	//console.log(fetchIconImage(node))
}

async function fetchIconImages(nodes) {
	nodes.forEach(n => {
		
	});
	const response = await fetch(request + "/image/" + "1")
	if(response.status== 200){
		await response.url.then(url => {
			return url
		});
	}
	return null
}

// fetch(myRequest).then(function(response) {
// 	console.log(response.status); // returns 200
// 	response.blob().then(function(myBlob) {
// 	  var objectURL = URL.createObjectURL(myBlob);
// 	  myImage.src = objectURL;
// 	});
//   });