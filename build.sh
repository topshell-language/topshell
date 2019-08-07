sbt fullOptJS && \
cd node && \
npm install && \
node_modules/.bin/pkg package.json && \
echo "All done."
