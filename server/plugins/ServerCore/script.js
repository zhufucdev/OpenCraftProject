let map = new Object();
server.getOnServerPing().add(
    (event) => {
        let hostName = event.getClient().getAddress().getHostName();
        if(map[hostName] === undefined){
            map[hostName] = 0;
        }
        let text;
        switch(map[hostName]){
            case 1: text = "Nothing will be displayed.";break;
            case 2: text = "Don't click more.";break;
            case 3: text = "I said STOP.";break;
            case 4: text = "Stop please...";break;
            case 5: text = "Okay.. I'll show you that...";break;
            default: text = "Your address is "+hostName;
        }
        event.setMotd(
            getColor('blue') + "OpenCraft™" + getColor('reset') + " Local Test\n" + text
        );
        event.setIcon(file.of(server.getDataFolder().getPath()+'/logo.png'))
        map[hostName] ++;
    }
)