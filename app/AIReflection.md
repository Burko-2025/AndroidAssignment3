1.a. For this specific assignment I mostly kept the AI generated code as- is. Since this was my first time building a gps, accelerometer 
and magnetic field sensors app I relied heavily on the help of the AI generated code to provide me with everything I needed to make the
app do what I needed it to do. Inside the activity_main.xml I did change some of the placements and wording used to create the layout of
the app

1.b. One example of something I chose to add that was not in the original code I used from AI was in the text box where I show if the distance
is unchanged or getting larger or smaller it was constantly changing because of the small variations you get when your using the GPS. I changed
it so that anything under a 3m change it did not register. Keeping everything more consistent

1.c. One thing I learned while making my app that we had not covered in class yet was how to calculate distances between two points and determine
how far it was from point a to point b and determine which direction you need to travel in order to get from one point to the other.

2.a. I verified the correctness of AI generated code by running the app and making sure that everything works the way I expected it to. By testing
everything one step at a time it makes sure that every piece of code that has been added it does what it is supposed to do.

2.b. One key change I made was to add in the small variation code that prevents my distance unchanged text from constantly changing to larger or smaller 
because of the fluxation in the reading from the gps. I changed this because when your staying in the same position and the gps is changing slightly 
it is not constantly adjusting text even though you are not actually moving. By implementing a certain distance it has to travel to activate that it
made the app less glitchy.

3.a. I have levelled up my understanding on how gps locating works. By using it in this assignment I have figured out the nuisances that gps locating uses.
It has really made me consider all the possible uses for it in the future and how it can be utilized.

3.b. The things that went well during this assignment was the developing of the ui features attached to this assignment. It was easy to build the look
and feel of the assignment. The thing that was the hardest was the use of the GPS locater, it took a few tries but now I feel like I have a better understanding
of how to use it and the technical side behind it.