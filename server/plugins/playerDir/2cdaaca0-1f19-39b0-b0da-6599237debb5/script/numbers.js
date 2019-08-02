let a = b = 1;
for (let i = 0; i < 20; i++){
  print(a);print(b);
  a += b; b += a;
}
print(getColor('green') + "Done")