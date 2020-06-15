# Puck3
Development of Puck 3 DSL.

Update site for latest release : https://yanntm.github.io/Puck3/updates

Javadoc of the API : https://yanntm.github.io/Puck3/apidocs

Puck Metamodel : ![Metamodel](metamodel.jpg)

# 15 juin 2020
Prochaines étapes possibles.

Considérer des bases de code pour trouver un killer example.
Idée 1 : formaliser une contrainte pour chaque pricinpe SOLID sauf Liskov 
Remarque de Mikal : il faut contextualiser chaque contrainte en fonction des axes de changement donc je suis sceptique sur la faisabilité car le principe le plus général est caché ce qui change, donc cela présuppose de savoir ce qui change.
Débat : peut-on savoir a priori ce qui va changer ?

ISP : il est envisageable d'essayer de modifier une interface en fonction des besoins de tel ou tel client.
Mikal : oui mais ISP doit typiquement gérer un compromis entre souplesse et sécurité. Le bon compromis dépend de chaque cas.
Donc ISP doit etre paramétré par les méthodes à garder dans une interface. Mais alors qu'apporte une règle et un outil qui supporte ISP par rapport à faire tout à la main ?
Réponse : on détecte des incohérences et on fait l'évolution du code client et des classes qui implémentent l'interface pour s'adapter au changement d'interface.

DIP : on peut imaginer une heuristique qui avertit quand une classe concrète est utilisée directement.
Mikal : attention en fait cela ne pose souvent pas de problème, notamment quand la classe concrète n'est pas amenée à évoluer.
Il y aurait donc beaucoup de faux positifs.

On peut donc imaginer un niveau basique sans connaissance a priori sur les changements et un niveau plus avancé où l'outil affine ses remarques et propostions en fonction des hypothèses sur les changements.
