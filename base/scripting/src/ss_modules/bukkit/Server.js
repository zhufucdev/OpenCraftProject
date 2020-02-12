'use requireJava';
const bukkit = requireJava('org.bukkit.Bukkit');
const info = requireJava('com.zhufu.opencraft.Info');

function MultiOperations(operation, labels, defaultOp) {
    if (!labels) operation(defaultOp);
    else if (typeof labels === 'object') {
        for (let i in labels) {
            operation(labels[i])
        }
    } else if (typeof labels === 'string') operation(labels)
}

function OpenCraftServer(bukkit) {
    this.stop = () => {
        bukkit.shutdown()
    };
    this.reload = (what) => {
        function doReload(selection) {
            switch (selection) {
                case 'server':
                    bukkit.reload();
                    break;
                case 'minecraft':
                    bukkit.reloadData();
                    break;
                case 'permission':
                    bukkit.reloadPermissions();
                    break;
                case 'command':
                    bukkit.reloadCommandAliases();
                    break;
            }
        }

        MultiOperations(doReload, what, 'server');
    };
    this.save = (what) => {
        function doSave(selection) {
            switch (selection) {
                case 'player':
                    bukkit.savePlayers();
                    break;
                case 'world':
                    const w = bukkit.getWorlds();
                    for (let i in w) {
                        w[i].save();
                    }
                    break;
                case 'info':
                    const list = info.cache;
                    for (let i in list) {
                        list[i].saveServerID()
                    }
                    break;
            }
        }

        MultiOperations(doSave, what, 'info')
    }
}

module.exports = new OpenCraftServer(bukkit);
