# Puck3
Development of Puck 3 DSL.

Update site for latest release : https://yanntm.github.io/Puck3/updates

Javadoc of the API : https://yanntm.github.io/Puck3/apidocs

Puck Metamodel : ![Metamodel](metamodel.jpg)

# réunion du 22 janvier

Envoie de la proposition de projet PSTL
https://www.overleaf.com/project/60002a0945cfbf178e743542
une étape du projet serait de simplifier du code présentant des conditions évaluables statiquement (true, false).

Issue 1  :  représentation canonique des conditionnelles.

Issue 2 : des condtions ne sont pas encore présentes. Voire il n'y en a pas encore. C'est un changement annoncé.
Ex frais de livraison au poids ou au montant.

Comment identifier les conditionnelles pertinentes.
Selon un papier indien les conditions des champs d'une classe dit control field peuvent etre identifier.
L'utilisateur pourrait fournir un ensemble de conditions booleennes (partition de preference)

On peut imaginer plusieurs cases à cocher (variantes de RCP)
sur la strategie par defaut,
sur l'injection d'une strategie par le constructeur,
sur l'injection par un setteur,

Dans l'exemple ci-dessous
if (modeLivraison.contentEquals("au poids")) {
			if (poids < 1000)
				return 5;
			else  return poids / 100;
		}
		else if (modeLivraison.contentEquals("au montant")) {
			if (montant > 100)
				return 0;
			else return 20;
		}
		else throw new IllegalArgumentException(modeLivraison);

La suppression de l'attribut modeLivraison pourrait être un refactoring à part

quelle signature pour la strategie (Objet initial ou données particulieres)

Sur gildedRose on a du isoler la conditionnelle dans une méthode qui lui est propre*

l'utilisateur se voit proposer la liste des tests booleens présent dans la conditionnelle
et il sélectionne les cas d'une partition
le code est alors réécrit
on duplique la fonction en introduisant chaque cas


# mail de Yann 13 janvier


quelques traces d'une recherche biblio sur refactor to polymorphism.
Je vous racontes le "gist" demain, c'est de la surface, i.e. une
selection sur les résultats de google, pas encore une vraie biblio (refs
de refs etc....).


** la littérature citable
Une "déf" par l'exemple (Fowler)
https://refactoring.com/catalog/replaceConditionalWithPolymorphism.html

Mais le livre "Refactoring" de Fowler en parles, j'ai pas le pdf

Outil JDeodorant + volée de papiers (la compétition :D)
https://github.com/tsantalis/JDeodorant
les papiers sont cités sur cette page d'accueil github

JDeodorant, un "concurrent" assez abouti sur les refactoring, mais c'est
open source et relativement propre ce que j'ai browsé, donc on peut le
hacker et s'appuyer dessus si on veut. C'est encore un peu actif et pas
mal de contributor.

dont ce papier en particulier sur ce sujet précis :
http://users.encs.concordia.ca/~nikolaos/publications/JSS_2010.pdf

plus récent là dessus (oopsla'18) et avec artefacts/outils
Identifying Refactoring Opportunities for Replacing TypeCode with
Subclass and State
http://www.cse.iitm.ac.in/~vjyothi/oopsla18.pdf

** intéressant car systématisé/suggère un "plan" de refactoring
Approche pour introduire la réecriture:
https://refactoring.guru/replace-conditional-with-polymorphism

Donc on propose des plans de refactoring, ici on suggère de commencer
par faire un Strategy avant de démarrer le pattern. L'approche est
"systématisé" même si ce n'est pas formel.

** Des tas d'exemples sur des blogs et autres, ici un ou deux choisis

Exemple commenté :
https://josdem.io/techtalk/refactoring/replace_conditional_with_polymorphism/

avec les classes qui embarquent des attributs, i.e. les branches
conditionnelles emmènent aussi des attributs.

https://sergeyzhuk.me/2017/02/25/replace-conditionals/
php, très classique switch/case => classes + factory qui garde le
switch.

Article 2005 : Refactor conditionals into polymorphism: what's the
performance cost of introducing virtual calls?

question intéressante en soi, mais article ancien

un autre article sur le thème
http://www.zndxzk.com.cn/down/2014/05_znen/31-1935-e132242.pdf
Automated pattern­directed refactoring for complex conditional
statements

mais pas d'outil

plus ancien mais pertinent ?
Automated refactoring to the Strategy design pattern
http://www2.aueb.gr/users/bzafiris/docs/cgzs12.pdf

# Rénion du 10 décembre
Retour sur GildedRose

discussion sur le refactoring automatique

on cherche a mesurer l'impact d'un changement

On est gêné pour exprimer la contrainte de couplage, notamment vis à vis de la granularité.

Une méthode est un seul noeud alors qu'on doit remanier le code à l'intéreur.

metrique de l'effort induit ?

systeme de gestion de sources ?

reutlisation vs modification

faire une experience de comparaison en introduisant l'enchanted item avec ou sans refactoring prealable

Trouver un autre exemple de kata, matrice creuse/pleine ?

Idée : contrainte specifiquement ce qui apparait dans les conditions (ex cacher name des conditionnelles dans gildedrose)

# Mini réunion 26 novembre
hide type Item  from pkg gildedrose; // this has no effect




# Mini réunion 5 novembre
La classe Item est un POD (Plain Old Data) mais on n'a pas le droit de la modifier.

Todo: écrire les règles weland pour faire respecter le cahhier des charges ci-dessous.
https://github.com/emilybache/GildedRose-Refactoring-Kata/blob/master/GildedRoseRequirements.txt


# réunion du 15 octobre

Détecter les cycles entre les paquetages eventuellement entre les classes.

Ecrire un papier. Valeur ajoutée = ?
Si on sait ce qu'on veut tout va bien mais comment amorcer la pompe ?
Des règles génériques ?
Exemple : cacher les paquetages .impl de tout le monde

Les règles sont elles bien audibles ?
Pourquoi se sert-on de Puck ?
paradoxe : pour ecrire les règles il faut déjà savoir ce qu'on veut

hypothèse : les clients sont capables d'écire les règles puck
patterns de règles  comme no cycle in ...
proposer architecture layered

études kata solid ou refactoring
https://kata-log.rocks/gilded-rose-kata
https://github.com/emilybache/Racing-Car-Katas
https://github.com/ivanbadia/solid-kata
https://kata-log.rocks/solid-principles

Mikal : il faut cacher ce qui bouge donc le client doit avoir une idée de ce qui bouge

Petit point sur les exemples qui ont été déjà été travaillés et qui viennent souvent de design pattern
Matrices : cacher les sortes de matrices

Katas : le code de départ ne respecte pas les principes solid
Q1 : peut on écrire les memes règles puck sur le système initial et final
Issue : en fait souvent il faut remanier aussi les règles

Question : classique en puck où placer les nouvelles classes parmis les ensembles déclarés ?

voir comment exprimer les regles avec des modules.




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
Il faudrait au moins enrichir la notion de concret/abstrait avec d'autres cas. nombre magique, code vs fonction, attributs vs classe, classe vs interface, if vs polymorphisme (replace condtionnal polymorphism)...
Prévoir des refectoring pour corriger ces erreurs typiques.


Trouver des arguments pour répondre à la critique attendue : si on connait les axes de changements pourquoi utiliser puck vu qu'alors je peux coder proprement.

Argument 1 : code decay. D'autres personnes voire le développeur initial peuvent par mégarde ou méconnaissance ne pas respecter les contraintes architecturales et abimer peu à peu l'architecture. L'outil permet à ton instant un audit pour détecter tout souci voire proposer des corrections. 

Argument 5 : Les règles documentent certaines hypothèses et contraintes pour les nouveaux dans l'équipe de développement.

Argument 2 : évolutions des besoins. A un moment donné telle architecture convient parce qu'on ne sait pas que tel aspect va changer. Quand on est prévenu il faut remanier l'architecture.

Argument 3 : c'est beaucoup plus facile de déclarer des axes de changement (ensembles weland) que de respecter des contraintes tout le temps.

Argument 4 : le fait d'expliciter les axes de changement permet justement d'en prendre conscience et de les respecter. Ceci est un argument pour la démarche que pour l'outil. 
Argument 4 bis : comparaison avec l'encapsulation. Tout le monde sait qu'il faut cacher les attributs d'une classe (les déclarer privés) mais le fait de les déclarer et que le compilateur fasse recpecter l'encapsulation est un vrai plus.

On peut donc imaginer un niveau basique sans connaissance a priori sur les changements et un niveau plus avancé où l'outil affine ses remarques et propositions en fonction des hypothèses sur les changements.

