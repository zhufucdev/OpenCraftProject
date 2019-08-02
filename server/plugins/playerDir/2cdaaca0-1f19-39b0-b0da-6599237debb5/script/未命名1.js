let husks = self.getLocation().getEntitiesNearby('crafthusk',10);for(let i = 0;i < 10; i++){
npc.create({
  name:'tester',
  spawnpoint: self.getLocation(),
attack:husks[Math.floor(Math.random()*husks.length)]
})};