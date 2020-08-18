const prettier = require("prettier");
const process = require('process');
// 0= idle 1= reading
var operatingMode = 0;
var javaText = "";

process.on('SIGINT', () => {
    switch(operatingMode){
        case 0:
            operatingMode = 1;
            break;
        case 1:
            let formattedText = prettier.format(javaText, {
                parser: "java",
                tabWidth: 2
              });
            console.log(formattedText);
            javaText="";
            operatingMode = 0;
            break;
    }
    console.error(operatingMode);
});

process.stdin.setEncoding("utf8");
process.stdin.on('readable', function() {
    // There is some data to read now.
    let data;
  
    while (data = this.read()) {
      javaText = javaText + data;
    }
  });
console.log(">READY<");