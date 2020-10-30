const prettier = require("prettier");
const process = require('process');
// 0= idle 1= reading
var operatingMode = 0;
var code = "";

process.on('SIGINT', () => {
    switch(operatingMode){
        case 0:
            operatingMode = 1;
            break;
        case 1:
            let formattedText = prettier.format(code, {
                parser: "css",
                tabWidth: 2,
				arrowParens: "avoid",
				printWidth: 90,
				singleQuote: false,
				semi: true,
				trailingComma: "none",
				useTabs: false
              });
            console.log(formattedText);
            code="";
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
      code = code + data;
    }
  });
console.log(">READY<");