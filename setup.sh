docker build . -t workspace:latest

docker run -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --name workspace \
  workspace

