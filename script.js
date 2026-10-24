let entities = [];

let relationships = [];

let currentEntity = null;


/* =========================================
   ATTRIBUTE CATEGORIES (symbols + sub-attributes)
========================================= */

const CATEGORY_INFO = {
    simple: {
        symbol: "●",
        label: "Simple",
        color: "#2563eb",
        hint: "Simple: one single value that cannot be split (e.g. Age)"
    },
    composite: {
        symbol: "▣",
        label: "Composite",
        color: "#9333ea",
        hint: "Composite: made of smaller parts (e.g. Name = First_Name + Last_Name)"
    },
    multivalued: {
        symbol: "◎",
        label: "Multivalued",
        color: "#ea580c",
        hint: "Multivalued: can hold many values (e.g. Phone_Numbers)"
    },
    derived: {
        symbol: "◌",
        label: "Derived",
        color: "#0d9488",
        hint: "Derived: calculated from other data (e.g. Age from Date_of_Birth)"
    }
};

let pendingSubAttributes = [];


function getCategoryInfo(attribute) {

    return CATEGORY_INFO[attribute.category] ||
        CATEGORY_INFO.simple;
}


function countAttributeRows(entity) {

    return entity.attributes.reduce(
        (total, attribute) =>
            total + 1 +
            (attribute.subAttributes || []).length,
        0
    );
}


function updateCategoryHint() {

    const category =
        document.getElementById("attributeCategory").value;

    const info = CATEGORY_INFO[category];

    const hint = document.getElementById("categoryHint");

    hint.textContent = info.symbol + "  " + info.hint;

    hint.style.borderLeftColor = info.color;
}


function toggleSubAttributeSection() {

    const category =
        document.getElementById("attributeCategory").value;

    document.getElementById("subAttributeSection")
        .style.display =
        category === "composite" ? "block" : "none";

    if (category !== "composite") {

        pendingSubAttributes = [];
    }

    renderPendingSubAttributes();

    updateCategoryHint();
}


function addSubAttribute() {

    const nameInput =
        document.getElementById("subAttributeName");

    const name = nameInput.value.trim();

    const type =
        document.getElementById("subAttributeType").value;

    const parentName =
        document.getElementById("attributeName").value.trim();


    if (name === "") {

        alert("Enter a sub-attribute name.");

        return;
    }

    if (
        parentName !== "" &&
        parentName.toLowerCase() === name.toLowerCase()
    ) {

        alert("A sub-attribute cannot have the same name as its parent attribute.");

        return;
    }

    if (
        pendingSubAttributes.some(
            sub => sub.name.toLowerCase() === name.toLowerCase()
        )
    ) {

        alert("Sub-attribute already added.");

        return;
    }

    pendingSubAttributes.push({
        name: name,
        type: type
    });

    nameInput.value = "";

    nameInput.focus();

    renderPendingSubAttributes();
}


function removeSubAttribute(index) {

    pendingSubAttributes.splice(index, 1);

    renderPendingSubAttributes();
}


function renderPendingSubAttributes() {

    const list =
        document.getElementById("subAttributeList");

    list.innerHTML = "";

    pendingSubAttributes.forEach(
        (sub, index) => {

            const row = document.createElement("div");

            row.className = "sub-attribute-item";

            row.innerHTML = `
                <span>└ ${sub.name} : ${sub.type}</span>
                <button
                    type="button"
                    class="sub-remove"
                    onclick="removeSubAttribute(${index})"
                    title="Remove">✕</button>
            `;

            list.appendChild(row);
        }
    );
}


function attributeSymbolHTML(attribute) {

    const info = getCategoryInfo(attribute);

    return `<span class="cat-symbol cat-${attribute.category || "simple"}" title="${info.label} attribute">${info.symbol}</span>`;
}


function attributeTagsHTML(attribute) {

    let tags = "";

    if (attribute.candidateKey) {

        tags += '<span class="attr-tag" title="Candidate key">CK</span>';
    }

    if (attribute.unique) {

        tags += '<span class="attr-tag" title="Unique">UQ</span>';
    }

    return tags;
}


function subAttributesHTML(attribute) {

    const subs = attribute.subAttributes || [];

    if (subs.length === 0) {

        return "";
    }

    let html = '<div class="sub-attributes">';

    subs.forEach(
        sub => {

            html += `<div class="sub-attribute">└ ${sub.name} : ${sub.type}</div>`;
        }
    );

    html += "</div>";

    return html;
}




function createEntity() {

    const name =
        document
            .getElementById("entityName")
            .value
            .trim();


    if (name === "") {

        alert("Enter an entity name.");

        return;
    }


    if (
        entities.some(
            entity =>
                entity.name.toLowerCase() ===
                name.toLowerCase()
        )
    ) {

        alert("Entity already exists.");

        return;
    }


    const entity = {

        name: name,

        attributes: []

    };


    entities.push(entity);

    currentEntity = entity;


    document.getElementById("entityName").value = "";


    renderEntities();

    updateEntityDropdowns();

    renderERDiagram();
}


function addAttribute() {

    if (!currentEntity) {

        alert("Create an entity first.");

        return;
    }


    const name =
        document
            .getElementById("attributeName")
            .value
            .trim();


    const type =
        document
            .getElementById("attributeType")
            .value;


    const isPK =
        document
            .getElementById("primaryKey")
            .checked;
    
    const category =
    document.getElementById("attributeCategory").value;

const isCandidateKey =
    document.getElementById("candidateKey").checked;

const isUnique =
    document.getElementById("uniqueAttribute").checked;



    if (name === "") {

        alert("Enter an attribute name.");

        return;
    }


    if (
        currentEntity.attributes.some(
            attribute =>
                attribute.name.toLowerCase() ===
                name.toLowerCase()
        )
    ) {

        alert("Attribute already exists.");

        return;
    }


    if (
        isPK &&
        currentEntity.attributes.some(
            attribute =>
                attribute.primaryKey
        )
    ) {

        alert(
            "This version supports one primary key per entity."
        );

        return;
    }


    if (
        category === "composite" &&
        pendingSubAttributes.length === 0
    ) {

        alert("Add at least one sub-attribute for a composite attribute.");

        return;
    }


    if (isPK && category !== "simple") {

        alert("A primary key must be a simple attribute.");

        return;
    }


    currentEntity.attributes.push({

        name: name,

        type: type,

        primaryKey: isPK,

        category: category,

        candidateKey: isCandidateKey,

        unique: isUnique,

        subAttributes:
            category === "composite"
                ? pendingSubAttributes.slice()
                : []

    });


    document.getElementById("attributeName").value = "";

    document.getElementById("primaryKey").checked = false;

    document.getElementById("attributeCategory").value = "simple";

    document.getElementById("candidateKey").checked = false;

    document.getElementById("uniqueAttribute").checked = false;

    pendingSubAttributes = [];

    toggleSubAttributeSection();


    renderEntities();

    renderERDiagram();
}



function renderEntities() {

    const container =
        document.getElementById("entities");


    container.innerHTML = "";


    if (entities.length === 0) {

        container.innerHTML =
            '<div class="empty">No entities created yet.</div>';

        return;
    }


    entities.forEach(
        (entity, index) => {

            const card =
                document.createElement("div");


            card.className = "entity";


            let html = `

                <div class="entity-title">
                    ${entity.name}
                </div>

                <div class="attributes">

            `;


            if (
                entity.attributes.length === 0
            ) {

                html += `

                    <div class="empty">
                        No attributes
                    </div>

                `;

            }


            entity.attributes.forEach(
                attribute => {

                    html += `

                        <div class="attribute
                            ${attribute.primaryKey
                                ? "pk"
                                : ""}">

                            <button class="attr-del" title="Delete attribute" onclick="deleteAttribute('${entity.name}', '${attribute.name}')">✕</button>
                            ${attributeSymbolHTML(attribute)}
                            ${attribute.name}
                            : ${attribute.type}
                            ${attributeTagsHTML(attribute)}
                            ${subAttributesHTML(attribute)}

                        </div>

                    `;

                }
            );


            html += `

                </div>

                <button
                    class="delete-btn"
                    onclick="deleteEntity(${index})">

                    Delete Entity

                </button>

            `;


            card.innerHTML = html;


            container.appendChild(card);

        }
    );
}



function deleteEntity(index) {

    const deletedEntity =
        entities[index];


    relationships =
        relationships.filter(
            relationship =>

                relationship.entityA !==
                deletedEntity.name &&

                relationship.entityB !==
                deletedEntity.name
        );


    if (
        currentEntity === deletedEntity
    ) {

        currentEntity = null;
    }


    entities.splice(index, 1);


    renderEntities();

    renderRelationships();

    updateEntityDropdowns();

    renderERDiagram();
}



function updateEntityDropdowns() {

    const entityA =
        document.getElementById("entityA");


    const entityB =
        document.getElementById("entityB");


    entityA.innerHTML =
        '<option value="">Select Entity</option>';

    entityB.innerHTML =
        '<option value="">Select Entity</option>';


    entities.forEach(
        entity => {

            const optionA =
                document.createElement("option");

            optionA.value = entity.name;

            optionA.textContent = entity.name;


            const optionB =
                document.createElement("option");

            optionB.value = entity.name;

            optionB.textContent = entity.name;


            entityA.appendChild(optionA);

            entityB.appendChild(optionB);

        }
    );
}


/* =========================================
   CREATE RELATIONSHIP
========================================= */

function createRelationship() {

    const name =
        document
            .getElementById("relationshipName")
            .value
            .trim();


    const entityA =
        document
            .getElementById("entityA")
            .value;


    const entityB =
        document
            .getElementById("entityB")
            .value;


    const cardinality =
        document
            .getElementById("cardinality")
            .value;


    if (name === "") {

        alert("Enter relationship name.");

        return;
    }


    if (
        entityA === "" ||
        entityB === ""
    ) {

        alert("Select both entities.");

        return;
    }


    if (entityA === entityB) {

        alert(
            "An entity cannot have a relationship with itself."
        );

        return;
    }


    if (
        relationships.some(
            relationship =>
                relationship.name.toLowerCase() ===
                name.toLowerCase()
        )
    ) {

        alert("Relationship already exists.");

        return;
    }


    relationships.push({

        name: name,

        entityA: entityA,

        entityB: entityB,

        cardinality: cardinality

    });


    document
        .getElementById("relationshipName")
        .value = "";

    document
        .getElementById("entityA")
        .value = "";

    document
        .getElementById("entityB")
        .value = "";


    renderRelationships();

    renderERDiagram();
}



function renderRelationships() {

    const container =
        document.getElementById("relationships");


    container.innerHTML = "";


    if (relationships.length === 0) {

        container.innerHTML =
            '<div class="empty">No relationships created yet.</div>';

        return;
    }


    relationships.forEach(
        (relationship, index) => {

            const card =
                document.createElement("div");


            card.className =
                "relationship-card";


            card.innerHTML = `

                <strong>
                    ${relationship.name}
                </strong>

                <span>

                    ${relationship.entityA}

                    ───

                    ${relationship.cardinality}

                    ───

                    ${relationship.entityB}

                </span>

                <button
                    class="delete-btn"
                    onclick="deleteRelationship(${index})">

                    Delete

                </button>

            `;


            container.appendChild(card);

        }
    );
}



function deleteRelationship(index) {

    relationships.splice(index, 1);

    renderRelationships();

    renderERDiagram();
}



function getPrimaryKey(entityName) {

    const entity =
        entities.find(
            entity =>
                entity.name === entityName
        );


    if (!entity) {

        return null;
    }


    return entity.attributes.find(
        attribute =>
            attribute.primaryKey
    );
}


function generateSchema(silent) {

    if (entities.length === 0) {

        if (!silent) { alert("Create at least one entity."); }

        return;
    }


    let schema = "";

    let sql = "";



    const notes = [];

    entities.forEach(
        entity => {

            const cols = [];
            const pkCols = [];
            const uniques = [];
            const multi = [];
            const derived = [];

            const entityPk =
                entity.attributes.find(a => a.primaryKey);


            entity.attributes.forEach(
                attr => {

                    const cat = attr.category || "simple";

                    /* IDEA 3: derived attributes are not stored */
                    if (cat === "derived") {

                        derived.push(attr.name);

                        notes.push({
                            cls: "cat-derived", icon: "◌",
                            text: `${entity.name}.${attr.name} is DERIVED → not stored. Calculate it with a query or view instead.`
                        });

                        return;
                    }

                    /* IDEA 2: multivalued attributes get their own table */
                    if (cat === "multivalued") {

                        multi.push(attr);

                        notes.push({
                            cls: "cat-multivalued", icon: "◎",
                            text: `${entity.name}.${attr.name} is MULTIVALUED → moved to a new table ${entity.name}_${attr.name} with a foreign key to ${entity.name}.`
                        });

                        return;
                    }

                    let own;

                    /* IDEA 1: composite attributes become columns */
                    if (
                        cat === "composite" &&
                        (attr.subAttributes || []).length > 0
                    ) {

                        own = attr.subAttributes.map(
                            sub => ({ name: sub.name, type: sub.type, tags: [] })
                        );

                        notes.push({
                            cls: "cat-composite", icon: "▣",
                            text: `${entity.name}.${attr.name} is COMPOSITE → flattened into columns: ${own.map(c => c.name).join(", ")}.`
                        });

                    } else {

                        own = [{ name: attr.name, type: attr.type, tags: [] }];
                    }

                    own.forEach(
                        col => {

                            if (attr.primaryKey) {
                                col.tags.push("PK");
                                pkCols.push(col.name);
                            }

                            if (attr.candidateKey) {
                                col.tags.push("CK");
                                col.notNull = true;
                            }

                            if (attr.unique) {
                                col.tags.push("UQ");
                            }

                            cols.push(col);
                        }
                    );

                    /* IDEA 4: candidate keys / unique attributes → UNIQUE */
                    if (
                        (attr.candidateKey || attr.unique) &&
                        !attr.primaryKey
                    ) {

                        uniques.push(own.map(c => c.name));

                        notes.push({
                            cls: "cat-simple", icon: "🔒",
                            text: `${entity.name}.${attr.name} is ${attr.candidateKey ? "a CANDIDATE KEY" : "UNIQUE"} → UNIQUE constraint${attr.candidateKey ? " + NOT NULL" : ""} added.`
                        });
                    }
                }
            );


            /* ---- relational schema line ---- */

            schema +=
                entity.name.toUpperCase() + "(" +
                cols.map(
                    c => c.name + (c.tags.length ? " " + c.tags.join(" ") : "")
                ).join(", ") + ")";

            if (derived.length) {

                schema += "   — derived, not stored: " + derived.join(", ");
            }

            schema += "\n\n";


            /* ---- main table SQL ---- */

            derived.forEach(
                d => {
                    sql += `-- ${entity.name}.${d} is derived (calculated, not stored)\n`;
                }
            );

            const lines = cols.map(
                c => `    ${c.name} ${c.type}${c.notNull ? " NOT NULL" : ""}`
            );

            if (pkCols.length) {

                lines.push(`    PRIMARY KEY (${pkCols.join(", ")})`);
            }

            uniques.forEach(
                group => {

                    lines.push(
                        `    CONSTRAINT uq_${entity.name}_${group.join("_")} UNIQUE (${group.join(", ")})`
                    );
                }
            );

            sql += `CREATE TABLE ${entity.name} (\n${lines.join(",\n")}\n);\n\n`;


            /* ---- IDEA 2: tables for multivalued attributes ---- */

            multi.forEach(
                attr => {

                    const table = `${entity.name}_${attr.name}`;

                    if (!entityPk) {

                        schema += `WARNING: ${table} needs a primary key on ${entity.name}.\n\n`;

                        return;
                    }

                    schema +=
                        `${table.toUpperCase()}(${entityPk.name} FK, ${attr.name}, PK(${entityPk.name}, ${attr.name}))\n\n`;

                    sql +=
                        `CREATE TABLE ${table} (\n` +
                        `    ${entityPk.name} ${entityPk.type} NOT NULL,\n` +
                        `    ${attr.name} ${attr.type} NOT NULL,\n` +
                        `    PRIMARY KEY (${entityPk.name}, ${attr.name}),\n` +
                        `    FOREIGN KEY (${entityPk.name}) REFERENCES ${entity.name}(${entityPk.name}) ON DELETE CASCADE\n` +
                        `);\n\n`;
                }
            );
        }
    );

    renderConversionNotes(notes);



    relationships.forEach(
        relationship => {

            const entityA =
                entities.find(
                    entity =>
                        entity.name ===
                        relationship.entityA
                );


            const entityB =
                entities.find(
                    entity =>
                        entity.name ===
                        relationship.entityB
                );


            if (!entityA || !entityB) {

                return;
            }


            const pkA =
                getPrimaryKey(entityA.name);


            const pkB =
                getPrimaryKey(entityB.name);


            if (!pkA || !pkB) {

                schema +=
                    `WARNING: ${relationship.name} requires primary keys.\n\n`;

                return;
            }



            if (
                relationship.cardinality ===
                "M:N"
            ) {

                schema +=
                    relationship.name.toUpperCase();


                schema +=
                    `(${pkA.name} FK, `;


                schema +=
                    `${pkB.name} FK, `;


                schema +=
                    `PK(${pkA.name}, ${pkB.name}))\n\n`;


                sql +=
                    `CREATE TABLE ${relationship.name} (\n`;


                sql +=
                    `    ${pkA.name} ${pkA.type},\n`;


                sql +=
                    `    ${pkB.name} ${pkB.type},\n`;


                sql +=
                    `    PRIMARY KEY (${pkA.name}, ${pkB.name}),\n`;


                sql +=
                    `    FOREIGN KEY (${pkA.name}) `;


                sql +=
                    `REFERENCES ${entityA.name}(${pkA.name}),\n`;


                sql +=
                    `    FOREIGN KEY (${pkB.name}) `;


                sql +=
                    `REFERENCES ${entityB.name}(${pkB.name})\n`;


                sql += ");\n\n";
            }


            

            else if (
                relationship.cardinality ===
                "1:N"
            ) {

                schema +=
                    `1:N → Add ${pkA.name} FK to ${entityB.name}\n\n`;


                sql +=
                    `ALTER TABLE ${entityB.name}\n`;


                sql +=
                    `ADD COLUMN ${pkA.name} ${pkA.type},\n`;


                sql +=
                    `ADD FOREIGN KEY (${pkA.name}) `;


                sql +=
                    `REFERENCES ${entityA.name}(${pkA.name});\n\n`;
            }


           

            else if (
                relationship.cardinality ===
                "1:1"
            ) {

                schema +=
                    `1:1 → Add ${pkA.name} FK to ${entityB.name}\n\n`;


                sql +=
                    `ALTER TABLE ${entityB.name}\n`;


                sql +=
                    `ADD COLUMN ${pkA.name} ${pkA.type} UNIQUE,\n`;


                sql +=
                    `ADD FOREIGN KEY (${pkA.name}) `;


                sql +=
                    `REFERENCES ${entityA.name}(${pkA.name});\n\n`;
            }

        }
    );


    document
        .getElementById("schema")
        .textContent =
        schema;


    document
        .getElementById("sql")
        .textContent =
        sql;
}



function copySQL() {

    const sql =
        document
            .getElementById("sql")
            .textContent;


    if (
        sql === "" ||
        sql === "SQL will appear here..."
    ) {

        alert("Generate SQL first.");

        return;
    }


    navigator.clipboard
        .writeText(sql)
        .then(() => {

            alert(
                "SQL copied to clipboard!"
            );

        })
        .catch(() => {

            alert(
                "Unable to copy SQL."
            );

        });
}




function renderERDiagram() {

    setTimeout(autoRefreshSchema, 0);


    const svg =
        document.getElementById("erDiagram");


    svg.innerHTML = "";


    if (entities.length === 0) {

        return;
    }



    const entityWidth = 220;

    const entityHeight = 150;

    const horizontalGap = 180;

    const tallestEntity = Math.max(
        entityHeight,
        ...entities.map(
            entity => 55 + countAttributeRows(entity) * 28
        )
    );

    const verticalGap =
        120 + Math.max(0, tallestEntity - entityHeight);


    const columns = Math.max(
        1,
        Math.ceil(Math.sqrt(entities.length))
    );


    const rows =
        Math.ceil(
            entities.length / columns
        );


    const diagramWidth =
        Math.max(
            800,
            columns *
            (entityWidth + horizontalGap)
        );


    const diagramHeight =
        Math.max(
            600,
            rows *
            (entityHeight + verticalGap)
        );


    svg.setAttribute(
        "width",
        diagramWidth
    );


    svg.setAttribute(
        "height",
        diagramHeight
    );



    const positions = {};


    entities.forEach(
        (entity, index) => {

            const column =
                index % columns;


            const row =
                Math.floor(
                    index / columns
                );


            const x =
                60 +
                column *
                (entityWidth + horizontalGap);


            const y =
                60 +
                row *
                (entityHeight + verticalGap);


            positions[entity.name] = {

                x: x,

                y: y,

                width: entityWidth,

                height:
                    Math.max(
                        entityHeight,
                        55 +
                        countAttributeRows(entity) *
                        28
                    )

            };

        }
    );


    relationships.forEach(
        relationship => {

            drawRelationship(
                svg,
                relationship,
                positions
            );

        }
    );


    entities.forEach(
        entity => {

            drawEntity(
                svg,
                entity,
                positions[entity.name]
            );

        }
    );
}



function drawEntity(
    svg,
    entity,
    position
) {

    const ns =
        "http://www.w3.org/2000/svg";


    const group =
        document.createElementNS(
            ns,
            "g"
        );


    const x =
        position.x;


    const y =
        position.y;


    const width =
        position.width;


    const height =
        position.height;


    const rect =
        document.createElementNS(
            ns,
            "rect"
        );


    rect.setAttribute(
        "x",
        x
    );


    rect.setAttribute(
        "y",
        y
    );


    rect.setAttribute(
        "width",
        width
    );


    rect.setAttribute(
        "height",
        height
    );


    rect.setAttribute(
        "rx",
        8
    );


    rect.setAttribute(
        "class",
        "entity-box"
    );


    group.appendChild(rect);


    const headerHeight = 40;


    const header =
        document.createElementNS(
            ns,
            "rect"
        );


    header.setAttribute(
        "x",
        x
    );


    header.setAttribute(
        "y",
        y
    );


    header.setAttribute(
        "width",
        width
    );


    header.setAttribute(
        "height",
        headerHeight
    );


    header.setAttribute(
        "rx",
        8
    );


    header.setAttribute(
        "class",
        "entity-header"
    );


    group.appendChild(header);


    const title =
        document.createElementNS(
            ns,
            "text"
        );


    title.setAttribute(
        "x",
        x + width / 2
    );


    title.setAttribute(
        "y",
        y + 25
    );


    title.setAttribute(
        "text-anchor",
        "middle"
    );


    title.setAttribute(
        "class",
        "entity-text"
    );


    title.textContent =
        entity.name.toUpperCase();


    group.appendChild(title);


    let rowIndex = 0;


    entity.attributes.forEach(
        attribute => {

            const info = getCategoryInfo(attribute);

            const rowY = y + 65 + rowIndex * 25;


            const symbol =
                document.createElementNS(ns, "text");

            symbol.setAttribute("x", x + 15);

            symbol.setAttribute("y", rowY);

            symbol.setAttribute("class", "attribute-text");

            symbol.style.fill = info.color;

            symbol.style.fontWeight = "bold";

            symbol.textContent = info.symbol;

            group.appendChild(symbol);


            const text =
                document.createElementNS(ns, "text");

            text.setAttribute("x", x + 34);

            text.setAttribute("y", rowY);

            text.setAttribute("class", "attribute-text");

            if (attribute.category === "derived") {

                text.style.fontStyle = "italic";
            }

            let label = attribute.name;

            if (attribute.primaryKey) {

                label = "🔑 " + label;
            }

            if (attribute.candidateKey) {

                label += " [CK]";
            }

            if (attribute.unique) {

                label += " [UQ]";
            }

            text.textContent = label;

            group.appendChild(text);

            rowIndex++;


            (attribute.subAttributes || []).forEach(
                sub => {

                    const subText =
                        document.createElementNS(ns, "text");

                    subText.setAttribute("x", x + 34);

                    subText.setAttribute(
                        "y",
                        y + 65 + rowIndex * 25
                    );

                    subText.setAttribute(
                        "class",
                        "attribute-text sub-attribute-text"
                    );

                    subText.textContent = "└ " + sub.name;

                    group.appendChild(subText);

                    rowIndex++;
                }
            );
        }
    );


    svg.appendChild(group);
}




function drawRelationship(
    svg,
    relationship,
    positions
) {

    const ns =
        "http://www.w3.org/2000/svg";


    const posA =
        positions[
            relationship.entityA
        ];


    const posB =
        positions[
            relationship.entityB
        ];


    if (!posA || !posB) {

        return;
    }


    const centerAX =
        posA.x +
        posA.width / 2;


    const centerAY =
        posA.y +
        posA.height / 2;


    const centerBX =
        posB.x +
        posB.width / 2;


    const centerBY =
        posB.y +
        posB.height / 2;


    /*
       Direction
    */

    let startX;
    let startY;

    let endX;
    let endY;


    if (
        Math.abs(centerBX - centerAX) >
        Math.abs(centerBY - centerAY)
    ) {

        if (centerBX > centerAX) {

            startX =
                posA.x +
                posA.width;

            startY =
                centerAY;


            endX =
                posB.x;

            endY =
                centerBY;

        } else {

            startX =
                posA.x;

            startY =
                centerAY;


            endX =
                posB.x +
                posB.width;

            endY =
                centerBY;

        }

    } else {

        if (centerBY > centerAY) {

            startX =
                centerAX;

            startY =
                posA.y +
                posA.height;


            endX =
                centerBX;

            endY =
                posB.y;

        } else {

            startX =
                centerAX;

            startY =
                posA.y;


            endX =
                centerBX;

            endY =
                posB.y +
                posB.height;

        }

    }


    const line1 =
        document.createElementNS(
            ns,
            "line"
        );


    line1.setAttribute(
        "x1",
        startX
    );


    line1.setAttribute(
        "y1",
        startY
    );


    line1.setAttribute(
        "x2",
        endX
    );


    line1.setAttribute(
        "y2",
        endY
    );


    line1.setAttribute(
        "class",
        "connection-line"
    );


    svg.appendChild(line1);


    const midX =
        (startX + endX) / 2;


    const midY =
        (startY + endY) / 2;


    const diamondSize = 55;


    const points = `

        ${midX},${midY - diamondSize / 2}

        ${midX + diamondSize / 2},${midY}

        ${midX},${midY + diamondSize / 2}

        ${midX - diamondSize / 2},${midY}

    `;


    const diamond =
        document.createElementNS(
            ns,
            "polygon"
        );


    diamond.setAttribute(
        "points",
        points
    );


    diamond.setAttribute(
        "class",
        "relationship-diamond"
    );


    svg.appendChild(diamond);

    const relationshipText =
        document.createElementNS(
            ns,
            "text"
        );


    relationshipText.setAttribute(
        "x",
        midX
    );


    relationshipText.setAttribute(
        "y",
        midY + 4
    );


    relationshipText.setAttribute(
        "text-anchor",
        "middle"
    );


    relationshipText.setAttribute(
        "class",
        "relationship-text"
    );


    relationshipText.textContent =
        relationship.name;


    svg.appendChild(
        relationshipText
    );

    const cardA =
        document.createElementNS(
            ns,
            "text"
        );


    cardA.setAttribute(
        "x",
        startX +
        (midX - startX) * 0.35
    );


    cardA.setAttribute(
        "y",
        startY +
        (midY - startY) * 0.35 -
        8
    );


    cardA.setAttribute(
        "class",
        "cardinality-text"
    );


    cardA.textContent =
        relationship.cardinality
            .split(":")[0];


    svg.appendChild(cardA);
    const cardB =
        document.createElementNS(
            ns,
            "text"
        );


    cardB.setAttribute(
        "x",
        endX +
        (midX - endX) * 0.35
    );


    cardB.setAttribute(
        "y",
        endY +
        (midY - endY) * 0.35 -
        8
    );


    cardB.setAttribute(
        "class",
        "cardinality-text"
    );


    cardB.textContent =
        relationship.cardinality
            .split(":")[1];


    svg.appendChild(cardB);
}


renderERDiagram();


/* Attribute category listeners */

document
    .getElementById("attributeCategory")
    .addEventListener("change", toggleSubAttributeSection);

document
    .getElementById("subAttributeName")
    .addEventListener("keydown", event => {

        if (event.key === "Enter") {

            event.preventDefault();

            addSubAttribute();
        }
    });

toggleSubAttributeSection();


/* =========================================
   UPGRADE: interactive features
========================================= */

function renderConversionNotes(notes) {

    const box = document.getElementById("conversionNotes");

    if (notes.length === 0) {

        box.innerHTML =
            '<div class="empty">All attributes are simple – each one became a plain column.</div>';

        return;
    }

    box.innerHTML = notes.map(
        note => `<div class="note-item"><b class="${note.cls}">${note.icon}</b> ${note.text}</div>`
    ).join("");
}


function autoRefreshSchema() {

    const toggle = document.getElementById("autoSchema");

    if (!toggle || !toggle.checked) {

        return;
    }

    if (entities.length > 0) {

        generateSchema(true);

    } else {

        document.getElementById("schema").textContent = "Schema will appear here...";

        document.getElementById("sql").textContent = "SQL will appear here...";

        document.getElementById("conversionNotes").innerHTML =
            '<div class="empty">Generate the schema to see the conversion steps.</div>';
    }
}


function deleteAttribute(entityName, attributeName) {

    const entity = entities.find(e => e.name === entityName);

    if (!entity) {

        return;
    }

    entity.attributes =
        entity.attributes.filter(a => a.name !== attributeName);

    renderEntities();

    renderERDiagram();
}


function downloadSQL() {

    const sql = document.getElementById("sql").textContent;

    if (sql === "" || sql === "SQL will appear here...") {

        alert("Generate SQL first.");

        return;
    }

    const blob = new Blob([sql], { type: "text/plain" });

    const link = document.createElement("a");

    link.href = URL.createObjectURL(blob);

    link.download = "schema.sql";

    link.click();

    URL.revokeObjectURL(link.href);
}


function loadExample() {

    if (
        entities.length > 0 &&
        !confirm("Replace your current model with the example?")
    ) {

        return;
    }

    entities = [
        {
            name: "Student",
            attributes: [
                { name: "Student_ID", type: "INT", primaryKey: true, category: "simple", candidateKey: false, unique: false, subAttributes: [] },
                { name: "Name", type: "VARCHAR(100)", primaryKey: false, category: "composite", candidateKey: false, unique: false,
                  subAttributes: [{ name: "First_Name", type: "VARCHAR(100)" }, { name: "Last_Name", type: "VARCHAR(100)" }] },
                { name: "Address", type: "VARCHAR(100)", primaryKey: false, category: "composite", candidateKey: false, unique: false,
                  subAttributes: [{ name: "House_Number", type: "INT" }, { name: "Street", type: "VARCHAR(100)" }, { name: "City", type: "VARCHAR(100)" }, { name: "PIN_Code", type: "INT" }] },
                { name: "Email", type: "VARCHAR(100)", primaryKey: false, category: "simple", candidateKey: true, unique: false, subAttributes: [] },
                { name: "Phone", type: "VARCHAR(100)", primaryKey: false, category: "multivalued", candidateKey: false, unique: false, subAttributes: [] },
                { name: "DOB", type: "DATE", primaryKey: false, category: "simple", candidateKey: false, unique: false, subAttributes: [] },
                { name: "Age", type: "INT", primaryKey: false, category: "derived", candidateKey: false, unique: false, subAttributes: [] }
            ]
        },
        {
            name: "Course",
            attributes: [
                { name: "Course_ID", type: "INT", primaryKey: true, category: "simple", candidateKey: false, unique: false, subAttributes: [] },
                { name: "Title", type: "VARCHAR(100)", primaryKey: false, category: "simple", candidateKey: false, unique: true, subAttributes: [] }
            ]
        }
    ];

    relationships = [
        { name: "Enrolls", entityA: "Student", entityB: "Course", cardinality: "M:N" }
    ];

    currentEntity = entities[0];

    renderEntities();

    renderRelationships();

    updateEntityDropdowns();

    renderERDiagram();

    generateSchema(true);
}
