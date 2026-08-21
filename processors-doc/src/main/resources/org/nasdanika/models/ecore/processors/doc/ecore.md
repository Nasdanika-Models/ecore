
```drawio-resource
ecore.drawio
```

**Ecore** is the metamodel at the heart of the [Eclipse Modeling Framework](https://eclipse.dev/emf/) (EMF): a small language for describing other languages, defined in terms of itself.
A dozen or so concepts - packages, classifiers, classes, attributes, references, operations - are enough to describe a family tree, a COBOL copybook, an org chart, a threat model, or Ecore itself.
This site is Ecore's own documentation, generated from `Ecore.ecore` by the generator in [this repository](https://github.com/Nasdanika-Models/ecore).

Ecore is also the bedrock of the [Nasdanika model tower](https://nasdanika.com/models.html).
Not a floor - what the ground is made of.
Every model in the tower, from [NxCore](https://nxcore.models.nasdanika.org/) at the base to the agentic stack on the roof, is an Ecore metamodel published as a Maven artifact, stored in Git, and documented by the generator that produced the page you are reading.

For a hands-on introduction with a worked example, see the [EMF Ecore quick start](https://docs.nasdanika.org/core/mapping/index.html#emf-ecore).

## What it is, briefly

| Layer | Concepts |
| --- | --- |
| Metamodel | [`EPackage`](references/eClassifiers/EPackage/index.html) - a namespace keyed by a URI. [`EClass`](references/eClassifiers/EClass/index.html), [`EDataType`](references/eClassifiers/EDataType/index.html), [`EEnum`](references/eClassifiers/EEnum/index.html) - classifiers. [`EAttribute`](references/eClassifiers/EAttribute/index.html) and [`EReference`](references/eClassifiers/EReference/index.html) - structural features, single or many valued; references may be containment and may have an opposite. [`EOperation`](references/eClassifiers/EOperation/index.html), [`EAnnotation`](references/eClassifiers/EAnnotation/index.html). Multiple inheritance, interfaces, abstract classes. |
| Model | `ResourceSet` - a group of resources with their factories and URI handlers. `Resource` - a group of objects identified by a URI. [`EObject`](references/eClassifiers/EObject/index.html) - an instance of an `EClass`. |

Two things in that table matter more than the rest.

**Resources are identified by URIs, not URLs.**
A model can be assembled from files, classpath resources, Git repositories, Maven artifacts, databases, or HTTP endpoints, and a reference may cross from one resource into another.
This is what makes federation possible: an architecture model can point into a model published by another team without either side merging repositories.

**Containment is part of the type system.**
An `EReference` marked as containment says the target object *lives here*, which gives every object a canonical location, a URI, a natural documentation hierarchy, and well-defined copy and delete semantics.
Ordinary object graphs have no such notion.

## Why bother when you already have X

Everything below follows from one property: **an Ecore metamodel is data at runtime, not just a compilation input.**

A running program can ask what classes exist, what features they have, which of them are containments, which are derived, which have opposites - and then read and write any object generically through `eGet` and `eSet`.
Tooling written once against Ecore works against every metamodel that will ever be written.
That is why one documentation generator documents forty models, one loader reads any of them from a Draw.io diagram, one differ diffs them, and one editor edits them.

**Java reflection** is the near miss, and worth naming first because it is the obvious objection.
Reflection describes *classes*, not *domains*: it cannot tell you that `children` is a containment rather than a plain list, that `father` is its opposite, that a value is derived rather than stored, or how an object is identified across files.
It also requires the classes to exist.
An Ecore metamodel can be loaded from a `.ecore` file at runtime and used without generating a line of code - which is how the generator that built this site documents metamodels it has never seen.

**Java records and POJOs.**
Excellent for data inside one program.
The tower needs objects that outlive the program: addressable by URI, editable by people who do not write Java, carrying provenance and documentation, and describable to a generic tool.
A record has no containment, no opposites, no change notification, no identity beyond the object reference, and nothing a documentation generator can read.
Ecore does not replace records and POJOs - it generates them, along with interfaces, factories, and the reflective plumbing.

**JPA / Hibernate.**
An ORM maps a fixed class model onto tables so a running application can persist state.
The concerns barely overlap.
Hibernate has no answer for "publish this vocabulary as a versioned artifact that another organization extends", for graphs spanning repositories, or for a diagram being an authoring surface.
Its metamodel exists to type queries, not to drive tooling.
Schema evolution is a migration script; metamodel evolution is a released version.
If a model needs a database, EMF persists to one - the question is which side is the source of truth, and in the tower it is Git.

**Spring domain models and DDD.**
Right instinct, wrong scope.
A ubiquitous language that lives in one service's entity classes is ubiquitous inside one team.
Ecore makes the same language a standalone artifact: versioned, dependency-managed, inheritable, documentable, and shareable across services, languages, and organizations.
DDD says define the vocabulary once; Ecore is a place to put it that is not a codebase.

**JSON Schema, OpenAPI, Protobuf, Avro.**
These describe *messages*.
They are strong at wire compatibility and weak at everything an in-memory graph needs: object identity, cross-document references, containment, bidirectional relationships, multiple supertypes, behavior.
Use them at the boundary - EMF reads and writes JSON and XML happily.
They are formats, not a modeling substrate.

**RDF/OWL, SHACL, property graphs.**
The genuine alternative, and the trade is deliberate.
RDF gives global identifiers and open-world reasoning at the cost of a heavy stack and weak code generation; property graphs give excellent traversal at the cost of a typed schema.
Ecore sits in between: a closed-world typed graph with generated Java, containment for structure, and Git for storage and branching.
Where reasoning is the goal, export - tower models project to RDF and to catalogs such as OSCAL and CycloneDX.

**UML tools and EA repositories** (Enterprise Architect, MagicDraw, LeanIX, Ardoq).
These bundle a fixed metamodel with a proprietary repository.
The metamodel is theirs, the storage is theirs, and documentation is a report you export.
Ecore inverts all three: your metamodel, your Git repository, documentation as a generated site.

None of this is an argument to stop using those tools.
It is an argument about where the *definition* of the domain lives.
Put it in Ecore, and the rest - Java classes, JSON schemas, database tables, documentation, diagrams, editors, validators - can be generated from it or mapped to it.

The honest cost: Ecore is Java-centric and carries Eclipse ancestry, XMI is not a format anyone enjoys reading by hand, and there is a learning curve before the payoff arrives.
It is twenty-year-old load-bearing technology, which is the point, and a technology choice all the same.

[Xcore](https://wiki.eclipse.org/Xcore) lowers the barrier of entry, especially with the Nasdanika [xcore archetype](https://github.com/Nasdanika-Archetypes/xcore-model) and AI assistance - the majority of the tower models were produced this way.

Also, there are loaders for Draw.io, Excel, YAML, Java sources, CSV, and PDF, and URI handlers for classpath, Git, GitLab, and Maven.

## What the tower gets from it

Most of what makes the tower a portfolio rather than a pile is Ecore mechanics:

* **Floors are `EPackage`s.** A micro-model is a Maven artifact because an Ecore package is one. Depending on a lower floor is a POM dependency.
* **Composition is `EClass` inheritance.** `Asset extends Element`, `Workable extends GovernedElement` - aspects accumulate along the inheritance chain, which is exactly why a model that takes its position in the tower stops defining what the floors below already provide.
* **Aspects are interfaces.** Multiple inheritance lets an element be documented, owned, staged, governed, and workable at once without a diamond of wrapper types.
* **Federation is URIs.** Cross-model references and merge-anchor URIs are resource mechanics, not custom plumbing.
* **Diagrams are resources.** The [Draw.io model](https://drawio.models.nasdanika.org/) makes a diagram file an Ecore resource, which is what turns a picture *of* the model into an authoring surface *for* it.
* **Features are named slots.** Model-level inheritance - add, replace, suppress against a base element - is possible because every feature of every object is addressable by name.

## Documentation generation

This is the Nasdanika contribution, and the reason this site exists.

### Metamodels document themselves

The generator in this repository walks an Ecore model as a graph and emits a static site: a page per package and classifier, declared and inherited features, operations with parameters, generated class and dependency diagrams in Draw.io and 2D/3D force-layout form, a [glossary](glossary.html), and full-text [search](search.html).
Prose written in Markdown or HTML resources alongside the metamodel - like this page - is woven in.
This is the concept of [micro-wikis](https://nasdanika.com/stories/2026/micro-wikis.html).
Every model site in the tower is produced by the same generator in the [Nasdanika CLI](https://docs.nasdanika.org/nsd-cli/index.html) and GitHub Pages.

A metamodel and its documentation have value before a single instance exists: they are where an organization's [ubiquitous language](https://martinfowler.com/bliki/UbiquitousLanguage.html) is written down in a form both people and programs can read.

### Models link back to their metamodels

The second half is what makes the first half pay off in practice.

When a *model* site is generated, the model elements and their `EClass`es are nodes in one graph.
Each element's page asks its class for a help decorator, and the class - documented by the generator described above - hands back a link to its own page.
The result is the small question mark to the right of every title as in this demo - [`research_task`](https://nasdanika-demos.github.io/latest-ai-development-swimlanes/references/tasks/research_task/index.html) - click it and you land on the definition of an agent *task*.

This matters more inside organizations than it looks.
A **System** at Acme Inc. is not a System at Omega LLC.
A **Capability**, a **Control**, a **Risk**, an **Owner** - each is a word people use confidently and mean differently, and the resulting misunderstandings are expensive and slow to surface.
Corporate glossaries exist to fix this and are read by nobody, because they live somewhere else.

Putting the definition one click from the thing being defined changes that.
The reader does not go looking for the vocabulary; the vocabulary is attached to what they are already reading. 
Onboarding, audits, handovers between consultants and clients, and AI agents grounding themselves in an unfamiliar estate all benefit from the same link - and because both sides are generated from the same source, the definition cannot drift from the thing it defines.

Add versioning to it - different versions of metamodel documentation published at different URLs.
Model sites generated against different metamodel versions would point to different definitions.
Javadoc.io does it natively, a glossary in Confluence doesn't.

## Applications

Beyond the tower, an Ecore metamodel plus generated documentation is a good answer whenever a vocabulary has to be shared and to outlive its authors:

* **Domain definition.** A ubiquitous language published as a versioned artifact that services, teams, and partner organizations extend rather than copy.
* **Format modeling.** Existing schemas and file formats become models: [Maven POMs](https://maven.models.nasdanika.org/), [XSD](https://xsd.models.nasdanika.org/), [Draw.io](https://drawio.models.nasdanika.org/), PowerPoint, Visio, PDF. A build file becomes an architecture element; a diagram becomes a queryable graph.
* **Legacy modernization.** Copybooks, [TIBCO BW5](https://bw5.models.nasdanika.org/) processes, and stored procedures parsed into typed models, so the register of what exists cannot drift from the code it describes.
* **AI grounding.** A typed graph with provenance is checkable in a way a wiki is not: an assistant's answer cites elements, and elements cite their sources and commits.
* **Teaching and onboarding.** The [Family model](https://family.models.nasdanika.org/) and the other demo models exist for this - a metamodel small enough to read in a sitting, with a model, a diagram, and a generated site to match.
