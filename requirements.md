## What it does
There is a long-running inside joke in my family. If I make a mistake that impacts my wife Daphne, I will offer her "pardons". I will choose a certain number of pardons based on how severe I think the mistake is. So if I mess up something small, I might offer her 3 pardons. But if I mess up something really big, I might offer her 243 pardons (for example). This is an example of offerring pardons.

There's also a version of this where the person who has been impacted can request pardons. So if Daphne makes a mistake that impacts me, I would say to her "How many pardons?" And she would reply with a number that she thinks is appropriate. I can then either accept those pardons or request a different number, sort of like a negotiation.

We are building a web application that supports this.

## User flows
 - Flow 1: The user who made the mistake selects someone to send a certain number of Pardons to. As part of this, they describe what the Pardons are for.
 - Flow 2: A user impacted by a mistake requests a certain number of Pardons from the user who made the mistake. As part of this, they describe why they are requesting Pardons.
 - Flow 3: A user receives an offer of Pardons via in-app or email notification with a link to the offer. They can either accept the offer, reject the offer, or request a different number of Pardons.
 - Flow 4: A user receives a request for a certain number of Pardons via in-app or email notification with a link to the request. They can either accept the request, reject the request, or counter with a different number of Pardons they are offering.

## Tech stack
 - Frontend: open
 - Backend: Cloud Run
 - Database: Google Cloud Firestore
 - Hosting: Firebase Hosting
 - Authentication: Google Accounts
 - Email sending: SendGrid

## Constraints
 - Must work on mobile and desktop browsers
 - Only accessible to the members of my immediate family: max.ross@gmail.com, daphne.ross@gmail.com, violet.ross@gmail.com
 - Each pardon offer/request is standalone (no cumulative tally)
 - Each side gets up to two counter-offers per transaction, then must accept or reject
 - Frontend: React
 - Backend: TypeScript/Node on Cloud Run
