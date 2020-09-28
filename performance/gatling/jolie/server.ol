

interface BasicHttpInterface {
    RequestResponse: 
     post(undefined)(undefined),
     get(undefined)(undefined),

}


service TestService {
    execution{ concurrent}

inputPort TestPort {
       Location:  "socket://localhost:80"
       Protocol: http {
           .default.get = "get";
           .default.post = "post" 
       }
       Interfaces: BasicHttpInterface
    }
init
  {
    global.numberFiles=0

  } 
 main {
 
    [post(request)(response){

        synchronized( syncToken ) { 
            global.document[++global.numberDocuments] << request
        }
    }]
    
    [get(request)(response){
       response << global.document[1]
    }]
 }
 

}