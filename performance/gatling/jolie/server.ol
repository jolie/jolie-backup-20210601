include "file.iol"



interface BasicHttpInterface {
    RequestResponse: 
     put(undefined)(undefined),
     get(undefined)(undefined),

}


service TestService {
    execution{ concurrent}

inputPort TestPort {
       Location:  "socket://localhost:80"
       Protocol: http {
           .default.get = "get";
           .default.put = "put" 
       }
       Interfaces: BasicHttpInterface
    }
init
  {
    global.numberFiles=0

  } 
 main {
 
    [put(request)(reponse){

        synchronized( syncToken ) { 
            writeFileRequest.filename =  ++global.numberFile + ".jpg"
            writeFileRequest.format = "binary"
            writeFile@File(writeFileRequest)(writeFileResponse)
        }
    }]
    
    [get(request)(reponse){
                readFileRequest.filename =   "1.jpg"
                readFileRequest.format = "binary"
                writeFile@File(writeFileRequest)(writeFileResponse)
    }]
 }
 

}