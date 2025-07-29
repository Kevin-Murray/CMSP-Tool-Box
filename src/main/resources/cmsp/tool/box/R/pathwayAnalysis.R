
# Pathway Analysis R Script as part of CMSP Tool Box software
# @author: Kevin-Murray

# Installation ------------------------------------------------------------

# Run this code segment for a fresh R installation
# Be sure to run script using test files to ensure all libraries and 
# permission are set properly.
if (FALSE) {
  # Bioconductor requirments
  pkgs <- c("org.Rn.eg.db", "org.Mm.eg.db", "org.Hs.eg.db", "clusterProfiler",
            "AnnotationHub", "enrichplot", "KEGGREST", "UniProt.ws", 
            "ReactomePA")
  install.packages("BiocManager")
  BiocManager::install(pkgs)
  
  # CRAN requirements
  pkgs <- c("ggplot2", "ggrepel", "stringr", "Cairo", "grid", "cowplot",
            "readr", "purrr", "magrittr")
  install.packages(pkgs)
}


# Libraries ---------------------------------------------------------------

suppressMessages(library(org.Rn.eg.db))
suppressMessages(library(org.Mm.eg.db))
suppressMessages(library(org.Hs.eg.db))
suppressMessages(library(clusterProfiler))
suppressMessages(library(ggplot2))
suppressMessages(library(ggrepel))
suppressMessages(library(stringr))
suppressMessages(library(Cairo))
suppressMessages(library(grid))
suppressMessages(library(AnnotationHub))
suppressMessages(library(cowplot))
suppressMessages(library(enrichplot))
suppressMessages(library(readr))
suppressMessages(library(purrr))
suppressMessages(library(magrittr))
suppressMessages(library(KEGGREST))
suppressMessages(library(UniProt.ws))
suppressMessages(library(ReactomePA))


# Functions ---------------------------------------------------------------

#' Get Organism Gene Ontology (GO) Database
#'
#' @param organism string organism name
#'
#' @returns database object
#' 
get_goDB <- function(organism) {
  
  switch (organism,
          "Homo sapiens" = org.Hs.eg.db,
          "Mus musculus" = org.Mm.eg.db,
          "Rattus norvegicus" = org.Rn.eg.db
  )
}

#' Get organism KEGG Database code
#'
#' @param organism string organism name
#'
#' @returns String organism code
#' 
get_keggCode <- function(organism) {
  
  switch (organism,
          "Homo sapiens" = "hsa",
          "Mus musculus" = "mmu",
          "Rattus norvegicus" = "rno")
}

#' Get organism REACTOME Database code
#'
#' @param organism String - organism name
#'
#' @returns String - organism code
get_organism <- function(organism) {
  
  switch (organism,
          "Homo sapiens" = "human",
          "Mus musculus" = "mouse",
          "Rattus norvegicus" = "rat")
}

#' Format outputs of ORA-GO analyses
#'
#' @param resultList List of analysis results
#' @param data Protein report
#' @param db Organism database
#'
#' @returns Data.frame of formatted results
#' 
format_oraGO <- function(resultList, data, db) {
  
  # Merge reports by ID and Description columns
  oraFrame <- resultList[[1]]
  if (length(resultList) > 1) {
    for (i in 2:length(resultList)) {
      oraFrame <- merge(oraFrame, resultList[[i]], by = c("ID", "Description"), all = TRUE)
    }
  }
  
  # Remove dummy entries
  if ("GO:0000000" %in% oraFrame$ID) {
    oraFrame <- oraFrame[-which(oraFrame$ID == "GO:0000000"),]
  }
  
  # Get all database associated records with detected pathway IDs
  oraFrame$Definition <- NA
  oraFrame$Total.Detected <- NA
  oraFrame$BgRatio <- NA
  retrieved <- AnnotationDbi::select(db, keytype="GOALL", keys=oraFrame$ID, columns="UNIPROT")
  
  # For each ID in results, get associated meta data and format record
  for (i in 1:nrow(oraFrame)) {
    
    goTerm <- oraFrame$ID[i]
    oraFrame$Definition[i] <- Definition(goTerm)
    
    # Get unique proteins associated with pathway ID
    annot <- retrieved[which(retrieved$GOALL == goTerm),]
    if(any(duplicated(annot$UNIPROT))) {
      annot <- annot[-which(duplicated(annot$UNIPROT)),]
    }
    
    # Get total number of detected proteins from full protein report
    annot <- annot[which(!is.na(annot$UNIPROT)),]
    tmp <- match(annot$UNIPROT, data$Accession)
    oraFrame$Total.Detected[i] <- length(tmp[!is.na(tmp)])
    
    # Get total number of unique proteins in pathway
    index <- grep("BgRatio", colnames(oraFrame))
    tmp <- unlist(oraFrame[i, index])
    oraFrame$BgRatio[i] <- nrow(annot)
  }
  
  # Format gene ratio from character string to number
  indexG <- grep("GeneRatio:", colnames(oraFrame), fixed = T)
  for(i in 1:length(indexG)) {
    for (j in 1:nrow(oraFrame)) {
      
      tmp <- oraFrame[j, indexG[i]]
      
      num <- strsplit(tmp, split = "/", fixed = T)[[1]][1]
      den <- strsplit(tmp, split = "/", fixed = T)[[1]][2]
      
      oraFrame[j, indexG[i]] <- round(as.numeric(num) / as.numeric(den), 3)
    }
  }
  
  # Remove unused columns
  oraFrame <- oraFrame[-grep("BgRatio:", colnames(oraFrame), fixed = T)]
  oraFrame <- oraFrame[-grep("pvalue:", colnames(oraFrame), fixed = T)]
  
  # Create pathway coverage column
  oraFrame$Coverage <- round(oraFrame$Total.Detected / oraFrame$BgRatio * 100)
  
  # Filter data frame column order
  oraFrame <- oraFrame |> 
    dplyr::select("ID", "Description", "Definition", "BgRatio", "Total.Detected", "Coverage", everything())
  
  # Organize and group similar columns
  oraFrame <- oraFrame[c(1:6, 
                         grep("Count", colnames(oraFrame)),
                         grep("GeneRatio:", colnames(oraFrame)),
                         grep("p.adjust", colnames(oraFrame)),
                         grep("qvalue:", colnames(oraFrame)),
                         grep("geneID", colnames(oraFrame)))]
  
  # Rename columns
  colnames(oraFrame)[1:3] <- c("Pathway ID", "Pathway Definition", "Pathway Description")
  colnames(oraFrame)[4] <- paste0("Gene Set Size")
  colnames(oraFrame)[5] <- paste0("Detected Proteins")
  colnames(oraFrame)[6] <- paste0("Pathway Coverage [%]")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "Count:", "Significant Outcomes:")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "GeneRatio:", "Gene Ratio:")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "p.adjust:", "Adj. P-Value (BH):")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "qvalue:", "Adj. P-Value (Q-Value):")
  
  # Filter proteins associated with pathway to indicate if they were
  # signficantly up- or down-regulated. 
  indexG <- grep("geneID:", colnames(oraFrame), fixed = T)
  indexP <- grep("Adj. P-Value", colnames(data), fixed = T)
  indexR <- grep("Abundance Ratio:", colnames(data), fixed = T)
  for (i in 1:length(indexG)) {
    
    ratioName <- trimws(str_replace(colnames(oraFrame)[indexG[i]], "geneID:", ""))
    
    oraFrame[paste0("Num. Significant Upregulated: ", ratioName)] <- 0
    oraFrame[paste0("Num. Significant Downregulated: ", ratioName)] <- 0
    oraFrame[paste0("Significant Upregulated Accessions: ", ratioName)] <- ""
    oraFrame[paste0("Significant Downregulated Accessions: ", ratioName)] <- ""
    
    for (j in 1:nrow(oraFrame)) {
      tmp <- oraFrame[j, indexG[i]]
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      
      frame <- data[match(tmp, data$Accession),]
      up <- frame[which(frame[[indexR[i]]] > 1 & frame[[indexP[i]]] <= 0.05),]
      down <- frame[which(frame[[indexR[i]]] < 1 & frame[[indexP[i]]] <= 0.05),]
      
      oraFrame[j, paste0("Significant Upregulated Accessions: ", ratioName)] <- paste0(up$Accession, collapse = "/")
      oraFrame[j, paste0("Significant Downregulated Accessions: ", ratioName)] <- paste0(down$Accession, collapse = "/")
      oraFrame[j, paste0("Num. Significant Upregulated: ", ratioName)] <- nrow(up)
      oraFrame[j, paste0("Num. Significant Downregulated: ", ratioName)] <- nrow(down)
    }
  }
  
  oraFrame <- oraFrame[-grep("geneID", colnames(oraFrame))]
  
  return(oraFrame)
}

#' Format outputs of GSE-GO analyses
#'
#' @param resultList List of analysis results
#' @param data Protein report
#' @param db Organism database
#'
#' @returns Data.frame of formatted results
#' 
format_gseGO <- function(resultList, data, db) {
  
  # Merge results by ID and Description columns
  gseFrame <- resultList[[1]]
  if (length(resultList) > 1) {
    for (i in 2:length(resultList)) {
      gseFrame <- merge(gseFrame, resultList[[i]], by = c("ID", "Description"), all = TRUE)
    }
  }
  
  # Remove dummy pathway IDs
  if ("GO:0000000" %in% gseFrame$ID) {
    gseFrame <- gseFrame[-which(gseFrame$ID == "GO:0000000"),]
  }
  
  # Get all protein records associated with pathway ID
  gseFrame$Definition <- NA
  gseFrame$TotalSize <- NA
  gseFrame$Total.Detected <- NA
  retrieved <- AnnotationDbi::select(db, keys = keys(db), columns = c("GOALL", "UNIPROT"))
  
  # Loop through results and format records
  for (i in 1:nrow(gseFrame)) {
    
    goTerm <- gseFrame$ID[i]
    gseFrame$Definition[i] <- Definition(goTerm)
    
    # Filter pathways records to unique proteins
    annot <- retrieved[which(retrieved$GOALL == goTerm),]
    if(any(duplicated(annot$UNIPROT))) {
      annot <- annot[-which(duplicated(annot$UNIPROT)),]
    }
    
    # Filter the total number of proteins in pathway and total number of proteins
    # detected in full protein report
    annot <- annot[which(!is.na(annot$UNIPROT)),]
    tmp <- match(annot$UNIPROT, data$Accession)
    gseFrame$Total.Detected[i] <- length(tmp[!is.na(tmp)])
    gseFrame$TotalSize[i] <- length(unique(annot$UNIPROT)) - 1
  }
  
  # Remove unused columns
  gseFrame <- gseFrame[-grep("enrichmentScore:", colnames(gseFrame))]
  gseFrame <- gseFrame[-grep("pvalue:", colnames(gseFrame))]
  gseFrame <- gseFrame[-grep("rank:", colnames(gseFrame))]

  # Create pathway coverage variable
  gseFrame$Coverage <- round(gseFrame$Total.Detected / gseFrame$TotalSize * 100)
  
  # Filter data frame column order
  gseFrame <- gseFrame |> 
    dplyr::select("ID", "Description", "Definition", "TotalSize", "Total.Detected", "Coverage", everything())
  
  # Group and organize similar column types
  gseFrame <- gseFrame[c(1:6, 
                         grep("setSize", colnames(gseFrame)),
                         grep("NES", colnames(gseFrame)),
                         grep("p.adjust:", colnames(gseFrame)),
                         grep("qvalue:", colnames(gseFrame)),
                         grep("leading_edge:", colnames(gseFrame)),
                         grep("core_enrichment:", colnames(gseFrame)))]
  
  # Rename columns
  colnames(gseFrame)[1:3] <- c("Pathway ID", "Pathway Definition", "Pathway Description")
  colnames(gseFrame)[4] <- paste0("Gene Set Size")
  colnames(gseFrame)[5] <- paste0("Total Detected")
  colnames(gseFrame)[6] <- paste0("Pathway Coverage [%]")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "setSize:", "Enriched Proteins:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "NES:", "Normalized Enrichment Score:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "p.adjust:", "Adj. P-Value (BH):")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "qvalue:", "Adj. P-Value (Q-Value):")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "leading_edge:", "Leading Edge Metrics:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "core_enrichment:", "Core Enriched Proteins:")
  
  # Filter enriched proteins to indicate which records were signficantly up- or
  # down-regulated in associated ratio
  indexG <- grep("Core Enriched Proteins:", colnames(gseFrame), fixed = T)
  indexP <- grep("Adj. P-Value", colnames(data), fixed = T)
  indexR <- grep("Abundance Ratio:", colnames(data), fixed = T)
  for (i in 1:length(indexG)) {
    
    ratioName <- trimws(str_replace(colnames(gseFrame)[indexG[i]], "Core Enriched Proteins:", ""))
    
    gseFrame[paste0("Num. Significant Upregulated: ", ratioName)] <- 0
    gseFrame[paste0("Num. Significant Downregulated: ", ratioName)] <- 0
    gseFrame[paste0("Significant Upregulated Accessions: ", ratioName)] <- ""
    gseFrame[paste0("Significant Downregulated Accessions: ", ratioName)] <- ""
    
    for (j in 1:nrow(gseFrame)) {
      tmp <- gseFrame[j, indexG[i]]
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      
      frame <- data[match(tmp, data$Accession),]
      up <- frame[which(frame[[indexR[i]]] > 1 & frame[[indexP[i]]] <= 0.05),]
      down <- frame[which(frame[[indexR[i]]] < 1 & frame[[indexP[i]]] <= 0.05),]
      
      gseFrame[j, paste0("Significant Upregulated Accessions: ", ratioName)] <- paste0(up$Accession, collapse = "/")
      gseFrame[j, paste0("Significant Downregulated Accessions: ", ratioName)] <- paste0(down$Accession, collapse = "/")
      gseFrame[j, paste0("Num. Significant Upregulated: ", ratioName)] <- nrow(up)
      gseFrame[j, paste0("Num. Significant Downregulated: ", ratioName)] <- nrow(down)
    }
  }
  
  return(gseFrame)
}

#' Format outputs of ORA-KEGG analyses
#'
#' @param resultList List of analysis results
#' @param data Protein report
#' @param db KEGG database code
#'
#' @returns Data.frame of formatted results
#' 
format_oraKegg <- function(resultList, data, db) {
  
  # Merge results by category, subcategory, pathway ID, and description
  oraFrame <- resultList[[1]]
  if (length(resultList) > 1) {
    for (i in 2:length(resultList)) {
      oraFrame <- merge(oraFrame, resultList[[i]], by = c("category", "subcategory", "ID", "Description"), all = TRUE)
    }
  }
  
  # Remove dummy pathway IDs
  if ("GO:0000000" %in% oraFrame$ID) {
    oraFrame <- oraFrame[-which(oraFrame$ID == "GO:0000000"),]
  }
  
  # Get all associated pathway records
  oraFrame$Definition <- NA
  oraFrame$Total.Detected <- NA
  oraFrame$BgRatio <- NA
  database <- download_KEGG(db, keyType = "uniprot")
  
  # Loop through results and associate pathway record information
  for (i in 1:nrow(oraFrame)) {
    
    # Get pathway meta information
    id <- oraFrame$ID[i]
    retrieved <- keggGet(id)
    oraFrame$Description[i] <- retrieved[[1]]$PATHWAY_MAP
    if (!is.null(retrieved[[1]]$DESCRIPTION)) {
      oraFrame$Definition[i] <- retrieved[[1]]$DESCRIPTION[1]
    }
    
    # Filter to unique protein accessions 
    annot <- database$KEGGPATHID2EXTID
    annot <- annot[which(annot$from == id),]
    if(any(duplicated(annot$to))) {
      annot <- annot[-which(duplicated(annot$to)),]
    }
    
    # Calculate total number of proteins associated with pathway in full protein
    # report
    annot <- annot[which(!is.na(annot$to)),]
    tmp <- match(annot$to, data$Accession)
    oraFrame$Total.Detected[i] <- length(tmp[!is.na(tmp)])
    
    # Filter total number of proteins records in pathway
    index <- grep("BgRatio:", colnames(oraFrame))[1]
    background <- oraFrame[1,index]
    background <- strsplit(background, "/", fixed = T)[[1]][2]
    
    oraFrame$BgRatio[i] <- paste0(nrow(annot), "/", background)
  }
  
  # Format gene ratio variable from string to numeric
  indexG <- grep("GeneRatio:", colnames(oraFrame), fixed = T)
  for(i in 1:length(indexG)) {
    for (j in 1:nrow(oraFrame)) {
      
      tmp <- oraFrame[j, indexG[i]]
      num <- strsplit(tmp, split = "/", fixed = T)[[1]][1]
      den <- strsplit(tmp, split = "/", fixed = T)[[1]][2]
      
      oraFrame[j, indexG[i]] <- round(as.numeric(num) / as.numeric(den), 3)
    }
  }
  
  # Drop unused columns
  oraFrame <- oraFrame[-grep("BgRatio:", colnames(oraFrame), fixed = T)]
  oraFrame <- oraFrame[-grep("pvalue:", colnames(oraFrame), fixed = T)]
  
  # Calculate total number of proteins in pathway
  totalGene <- sapply(oraFrame$BgRatio, function(x) strsplit(x, "/", fixed = T)[[1]][1])
  background <- strsplit(oraFrame$BgRatio[1], "/", fixed = T)[[1]][2]
  symbol <- length(unique(data$`Gene Symbol`))
  oraFrame$BgRatio <- as.numeric(totalGene)
  
  # Make pathway coverage variable
  oraFrame$Coverage <- round(oraFrame$Total.Detected / oraFrame$BgRatio * 100)
  
  # Filter data frame columns order
  oraFrame <- oraFrame |> 
    dplyr::select("ID", "category", "subcategory", "Description", "Definition", "BgRatio", "Total.Detected", Coverage,  everything())
  
  # Group and organize similar column types
  oraFrame <- oraFrame[c(1:8, 
                         grep("Count", colnames(oraFrame)),
                         grep("GeneRatio", colnames(oraFrame)),
                         grep("p.adjust", colnames(oraFrame)),
                         grep("qvalue:", colnames(oraFrame)),
                         grep("geneID", colnames(oraFrame)))]
  
  # Rename columns
  colnames(oraFrame)[1:5] <- c("Pathway ID", "Pathway Category", "Pathway Subcategory", "Pathway Definition", "Pathway Description")
  colnames(oraFrame)[6] <- paste0("Gene Set Size")
  colnames(oraFrame)[7] <- paste0("Total Detected")
  colnames(oraFrame)[8] <- paste0("Pathway Coverage [%]")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "Count:", "Significant Outcomes:")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "GeneRatio:", "Gene Ratio:")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "p.adjust:", "Adj. P-Value (BH):")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "qvalue:", "Adj. P-Value (Q-Value):")
  
  # For proteins associated with pathway, indicate if they were significantly up-
  # or down-regulated in specified ratio
  indexG <- grep("geneID:", colnames(oraFrame), fixed = T)
  indexP <- grep("Adj. P-Value", colnames(data), fixed = T)
  indexR <- grep("Abundance Ratio:", colnames(data), fixed = T)
  for (i in 1:length(indexG)) {
    
    ratioName <- trimws(str_replace(colnames(oraFrame)[indexG[i]], "geneID:", ""))
    
    oraFrame[paste0("Num. Significant Upregulated: ", ratioName)] <- 0
    oraFrame[paste0("Num. Significant Downregulated: ", ratioName)] <- 0
    oraFrame[paste0("Significant Upregulated Accessions: ", ratioName)] <- ""
    oraFrame[paste0("Significant Downregulated Accessions: ", ratioName)] <- ""

    for (j in 1:nrow(oraFrame)) {
      tmp <- oraFrame[j, indexG[i]]
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      
      frame <- data[match(tmp, data$Accession),]
      up <- frame[which(frame[[indexR[i]]] > 1 & frame[[indexP[i]]] <= 0.05),]
      down <- frame[which(frame[[indexR[i]]] < 1 & frame[[indexP[i]]] <= 0.05),]
      
      oraFrame[j, paste0("Significant Upregulated Accessions: ", ratioName)] <- paste0(up$Accession, collapse = "/")
      oraFrame[j, paste0("Significant Downregulated Accessions: ", ratioName)] <- paste0(down$Accession, collapse = "/")
      oraFrame[j, paste0("Num. Significant Upregulated: ", ratioName)] <- nrow(up)
      oraFrame[j, paste0("Num. Significant Downregulated: ", ratioName)] <- nrow(down)
    }
  }
  
  oraFrame <- oraFrame[-grep("geneID", colnames(oraFrame))]
  
  return(oraFrame)
}

#' Format outputs of GSE-KEGG analyses
#'
#' @param resultList List of analysis results
#' @param data Protein report
#' @param db KEGG database code
#'
#' @returns Data.frame of formatted results
#' 
format_gseKegg <- function(resultList, data, db) {
  
  # Merge results by pathway ID and description
  gseFrame <- resultList[[1]]
  if (length(resultList) > 1) {
    for (i in 2:length(resultList)) {
      gseFrame <- merge(gseFrame, resultList[[i]], by = c("ID", "Description"), all = TRUE)
    }
  }
  
  # Remove dummy pathway IDs
  if ("GO:0000000" %in% gseFrame$ID) {
    gseFrame <- gseFrame[-which(gseFrame$ID == "GO:0000000"),]
  }
  
  # Download associated pathway records
  gseFrame$Definition <- NA
  gseFrame$TotalSize <- NA
  gseFrame$Total.Detected <- NA
  gseFrame$category <- NA
  gseFrame$subcategory <- NA
  database <- download_KEGG(db, keyType = "uniprot")
  
  # For each pathway results, associate pathway records
  for (i in 1:nrow(gseFrame)) {
    
    id <- gseFrame$ID[i]
    retrieved <- keggGet(id)
    gseFrame$Description[i] <- retrieved[[1]]$PATHWAY_MAP
    
    # Associate KEGG classifications
    if(!is.null(retrieved[[1]]$CLASS)) {
      class <- strsplit(retrieved[[1]]$CLASS, ";", fixed = T)[[1]]
      gseFrame$category[i] <- trimws(class[1])
      gseFrame$subcategory[i] <- trimws(class[2])
    }
    
    # Associate pathway description
    if (!is.null(retrieved[[1]]$DESCRIPTION)) {
      gseFrame$Definition[i] <- retrieved[[1]]$DESCRIPTION[1]
    }
    
    # Filter to unique protein records
    annot <- database$KEGGPATHID2EXTID
    annot <- annot[which(annot$from == id),]
    if(any(duplicated(annot$to))) {
      annot <- annot[-which(duplicated(annot$to)),]
    }
    
    # Calculate number of proteins from full protein report associated with
    # pathway ID
    annot <- annot[which(!is.na(annot$to)),]
    tmp <- match(annot$to, data$Accession)
    gseFrame$Total.Detected[i] <- length(tmp[!is.na(tmp)])
    
    # Total number of unique proteins in pathway
    gseFrame$TotalSize[i] <- length(annot$to)
  }
  
  # Drop unused columns
  gseFrame <- gseFrame[-grep("enrichmentScore:", colnames(gseFrame))]
  gseFrame <- gseFrame[-grep("pvalue:", colnames(gseFrame))]
  
  # Make pathway coverage variable
  gseFrame$Coverage <- round(gseFrame$Total.Detected / gseFrame$TotalSize * 100)
  
  # Filter data frame columns
  gseFrame <- gseFrame |> 
    dplyr::select("ID","category", "subcategory", "Description", "Definition", "TotalSize", "Total.Detected", "Coverage", everything())
  
  # Group and organize similar column types
  gseFrame <- gseFrame[c(1:8, 
                         grep("setSize", colnames(gseFrame)),
                         grep("NES", colnames(gseFrame)),
                         grep("p.adjust:", colnames(gseFrame)),
                         grep("qvalue:", colnames(gseFrame)),
                         grep("leading_edge:", colnames(gseFrame)),
                         grep("core_enrichment:", colnames(gseFrame)))]
  
  # Rename columns
  colnames(gseFrame)[1:5] <- c("Pathway ID", "Pathway Category", "Pathway Subcategory", "Pathway Definition", "Pathway Description")
  colnames(gseFrame)[6] <- paste0("Gene Set Size")
  colnames(gseFrame)[7] <- paste0("Detected Proteins")
  colnames(gseFrame)[8] <- paste0("Pathway Coverage [%]")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "setSize:", "Enriched Proteins:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "NES:", "Normalized Enrichment Score:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "p.adjust:", "Adj. P-Value (BH):")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "qvalue:", "Adj. P-Value (Q-Value):")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "leading_edge:", "Leading Edge Metrics:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "core_enrichment:", "Core Enriched Proteins:")
  
  # For enriched proteins, determine if protein is significantly up- or down-
  # regulated in associated ratio test
  indexG <- grep("Core Enriched Proteins:", colnames(gseFrame), fixed = T)
  indexP <- grep("Adj. P-Value", colnames(data), fixed = T)
  indexR <- grep("Abundance Ratio:", colnames(data), fixed = T)
  for (i in 1:length(indexG)) {
    
    ratioName <- trimws(str_replace(colnames(gseFrame)[indexG[i]], "Core Enriched Proteins:", ""))
    
    gseFrame[paste0("Num. Significant Upregulated: ", ratioName)] <- 0
    gseFrame[paste0("Num. Significant Downregulated: ", ratioName)] <- 0
    gseFrame[paste0("Significant Upregulated Accessions: ", ratioName)] <- ""
    gseFrame[paste0("Significant Downregulated Accessions: ", ratioName)] <- ""
    
    for (j in 1:nrow(gseFrame)) {
      tmp <- gseFrame[j, indexG[i]]
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      
      frame <- data[match(tmp, data$Accession),]
      up <- frame[which(frame[[indexR[i]]] > 1 & frame[[indexP[i]]] <= 0.05),]
      down <- frame[which(frame[[indexR[i]]] < 1 & frame[[indexP[i]]] <= 0.05),]
      
      gseFrame[j, paste0("Significant Upregulated Accessions: ", ratioName)] <- paste0(up$Accession, collapse = "/")
      gseFrame[j, paste0("Significant Downregulated Accessions: ", ratioName)] <- paste0(down$Accession, collapse = "/")
      gseFrame[j, paste0("Num. Significant Upregulated: ", ratioName)] <- nrow(up)
      gseFrame[j, paste0("Num. Significant Downregulated: ", ratioName)] <- nrow(down)
    }
  }
  
  return(gseFrame)
}

#' Format outputs of ORA-REACTOME analyses
#'
#' @param resultList List of analysis results
#' @param data Protein report
#' @param organism Organism code
#' @param db GO database
#'
#' @returns Formatted data frame of merged results
format_oraREACTOME <- function(resultList, data, organism, db) {
  
  # Merge results by ID and description
  oraFrame <- resultList[[1]]
  if (length(resultList) > 1) {
    for (i in 2:length(resultList)) {
      oraFrame <- merge(oraFrame, resultList[[i]], by = c("ID", "Description"), all = TRUE)
    }
  }
  
  # Remove dummy pathway IDs
  if ("GO:0000000" %in% oraFrame$ID) {
    oraFrame <- oraFrame[-which(oraFrame$ID == "GO:0000000"),]
  }
  
  # Get associated pathway records
  oraFrame$Total.Detected <- NA
  oraFrame$BgRatio <- NA
  retrieved <- ReactomePA::gson_Reactome(organism)
  retrieved <- retrieved@gsid2gene
  
  # For each pathway result, associate pathway records
  for (i in 1:nrow(oraFrame)) {
    
    id <- oraFrame$ID[i]
    
    # Get unique proteins associated with pathway
    annot <- retrieved[which(retrieved$gsid == id),]
    if(any(duplicated(annot$gene))) {
      annot <- annot[-which(duplicated(annot$gene)),]
    }
    annot <- annot[which(!is.na(annot$gene)),]
    
    # Map entrez id to Uniprot accession, filter to unique
    mapped <- bitr(annot$gene, fromType = "ENTREZID", toType = "UNIPROT", OrgDb = db)
    uProt <- unique(mapped$UNIPROT)
    
    # Calculate the total number of proteins in full protein report that are
    # part of pathway
    tmp <- match(uProt, data$Accession)
    oraFrame$Total.Detected[i] <- length(tmp[!is.na(tmp)])
    
    # Total number of proteins in pathway
    oraFrame$BgRatio[i] <- length(uProt)
  }
  
  # Format gene ratio variable from string to numeric
  indexG <- grep("GeneRatio:", colnames(oraFrame), fixed = T)
  for(i in 1:length(indexG)) {
    for (j in 1:nrow(oraFrame)) {
      
      tmp <- oraFrame[j, indexG[i]]
      num <- strsplit(tmp, split = "/", fixed = T)[[1]][1]
      den <- strsplit(tmp, split = "/", fixed = T)[[1]][2]
      
      oraFrame[j, indexG[i]] <- round(as.numeric(num) / as.numeric(den), 3)
    }
  }
  
  # Format background ratio column
  # TODO - can this be deleted?
  background <- unlist(oraFrame[grep("BgRatio:", colnames(oraFrame), fixed = T)[1]])
  background <- background[which(!is.na(background))][1]
  background <- strsplit(background, "/", fixed = T)[[1]][2]
  
  # Drop unused columns
  oraFrame <- oraFrame[-grep("BgRatio:", colnames(oraFrame), fixed = T)]
  oraFrame <- oraFrame[-grep("pvalue:", colnames(oraFrame), fixed = T)]
  
  # Make pathway coverage variable
  oraFrame$Coverage <- round(oraFrame$Total.Detected / oraFrame$BgRatio * 100)
  
  # Format data frame columns
  oraFrame <- oraFrame |> 
    dplyr::select("ID", "Description", "BgRatio", "Total.Detected", "Coverage", everything())
  
  # Group and organize related column types
  oraFrame <- oraFrame[c(1:5, 
                         grep("Count", colnames(oraFrame)),
                         grep("GeneRatio", colnames(oraFrame)),
                         grep("p.adjust", colnames(oraFrame)),
                         grep("qvalue:", colnames(oraFrame)),
                         grep("geneID", colnames(oraFrame)))]
  
  # Rename columns
  colnames(oraFrame)[1:2] <- c("Pathway ID", "Pathway Definition")
  colnames(oraFrame)[3] <- paste0("Gene Set Size")
  colnames(oraFrame)[4] <- paste0("Detected Proteins")
  colnames(oraFrame)[5] <- paste0("Pathway Coverage [%]")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "Count:", "Significant Outcomes:")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "GeneRatio:", "Gene Ratio:")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "p.adjust:", "Adj. P-Value (BH):")
  colnames(oraFrame) <- str_replace_all(colnames(oraFrame), "qvalue:", "Adj. P-Value (Q-Value):")
  
  # For enriched proteins, determine if protein is significantly up- or down-
  # regulated in corresponding ratio test
  indexG <- grep("geneID:", colnames(oraFrame), fixed = T)
  indexP <- grep("Adj. P-Value", colnames(data), fixed = T)
  indexR <- grep("Abundance Ratio:", colnames(data), fixed = T)
  for (i in 1:length(indexG)) {
    
    ratioName <- trimws(str_replace(colnames(oraFrame)[indexG[i]], "geneID:", ""))
    
    oraFrame[paste0("Num. Significant Upregulated: ", ratioName)] <- 0
    oraFrame[paste0("Num. Significant Downregulated: ", ratioName)] <- 0
    oraFrame[paste0("Significant Upregulated Accessions: ", ratioName)] <- ""
    oraFrame[paste0("Significant Downregulated Accessions: ", ratioName)] <- ""

    for (j in 1:nrow(oraFrame)) {
      tmp <- oraFrame[j, indexG[i]]
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      
      frame <- data[which(data$Accession %in% tmp),]
      up <- frame[which(frame[[indexR[i]]] > 1 & frame[[indexP[i]]] <= 0.05),]
      down <- frame[which(frame[[indexR[i]]] < 1 & frame[[indexP[i]]] <= 0.05),]
      
      oraFrame[j, paste0("Significant Upregulated Accessions: ", ratioName)] <- paste0(up$Accession, collapse = "/")
      oraFrame[j, paste0("Significant Downregulated Accessions: ", ratioName)] <- paste0(down$Accession, collapse = "/")
      oraFrame[j, paste0("Num. Significant Upregulated: ", ratioName)] <- nrow(up)
      oraFrame[j, paste0("Num. Significant Downregulated: ", ratioName)] <- nrow(down)
    }
  }
  
  oraFrame <- oraFrame[-grep("geneID", colnames(oraFrame))]
  
  return(oraFrame)
}

#' Format outputs of GSE-REACTOME analyses
#'
#' @param resultList List of pathway analysis results
#' @param data Protein report
#' @param organism Organism code
#' @param db Gene Ontology database
#'
#' @returns Data frame of merged and formatted results
format_gseREACTOME <- function(resultList, data, organism, db) {
  
  # Merge results by pathway ID and description
  gseFrame <- resultList[[1]]
  if (length(resultList) > 1) {
    for (i in 2:length(resultList)) {
      gseFrame <- merge(gseFrame, resultList[[i]], by = c("ID", "Description"), all = TRUE)
    }
  }
  
  # Remove dummy pathway IDs
  if ("GO:0000000" %in% gseFrame$ID) {
    gseFrame <- gseFrame[-which(gseFrame$ID == "GO:0000000"),]
  }
  
  # Get associated pathway records
  gseFrame$Total.Detected <- NA
  gseFrame$BgRatio <- NA
  retrieved <- ReactomePA::gson_Reactome(organism)
  retrieved <- retrieved@gsid2gene
  
  # Convert entrez ID results to Uniprot accessions
  indexS <- grep("setSize", colnames(gseFrame), fixed = T)
  indexC <- grep("core_enrichment", colnames(gseFrame), fixed = T)
  for (i in 1:length(indexC)) {
    for (j in 1:nrow(gseFrame)) {
      
      tmp <- gseFrame[j, indexC[i]]
      
      if(is.na(tmp)) {
        next
      } 
      
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      mapped <- bitr(tmp, fromType = "ENTREZID", toType = "UNIPROT", OrgDb = db)
      uniprot <- unique(mapped$UNIPROT)
      uniprot <- uniprot[which(uniprot %in% data$Accession)]
      gseFrame[j, indexS[i]] <- length(uniprot)
      gseFrame[j, indexC[i]] <- paste0(uniprot, collapse = "/")
    }
  }
  
  # For pathway result, associate pathway record information
  for (i in 1:nrow(gseFrame)) {
    
    id <- gseFrame$ID[i]
    
    # Get unique protein records
    annot <- retrieved[which(retrieved$gsid == id),]
    if(any(duplicated(annot$gene))) {
      annot <- annot[-which(duplicated(annot$gene)),]
    }
    annot <- annot[which(!is.na(annot$gene)),]
    
    # Map entrez IDs to unique Uniprot accessions
    mapped <- bitr(annot$gene, fromType = "ENTREZID", toType = "UNIPROT", OrgDb = db)
    uProt <- unique(mapped$UNIPROT)
    
    # Calculate proteins detected in full protein report
    tmp <- match(uProt, data$Accession)
    gseFrame$Total.Detected[i] <- length(tmp[!is.na(tmp)])
    
    # Calculate total number of proteins in pathway
    gseFrame$BgRatio[i] <- length(uProt)
  }
  
  # Drop unused variables
  gseFrame <- gseFrame[-grep("enrichmentScore:", colnames(gseFrame))]
  gseFrame <- gseFrame[-grep("pvalue:", colnames(gseFrame))]
  
  # Calculate pathway coverage
  gseFrame$Coverage <- round(gseFrame$Total.Detected / gseFrame$BgRatio * 100)
  
  # Filter data frame columns
  gseFrame <- gseFrame |> 
    dplyr::select("ID", "Description", "BgRatio", "Total.Detected", "Coverage", everything())
  
  # Group and organize similar column types
  gseFrame <- gseFrame[c(1:5, 
                         grep("setSize", colnames(gseFrame)),
                         grep("NES", colnames(gseFrame)),
                         grep("p.adjust:", colnames(gseFrame)),
                         grep("qvalue:", colnames(gseFrame)),
                         grep("leading_edge:", colnames(gseFrame)),
                         grep("core_enrichment:", colnames(gseFrame)))]
  
  # Rename columns
  colnames(gseFrame)[1:2] <- c("Pathway ID", "Pathway Definition")
  colnames(gseFrame)[3] <- paste0("Gene Set Size")
  colnames(gseFrame)[4] <- paste0("Detected Proteins")
  colnames(gseFrame)[5] <- paste0("Pathway Coverage [%]")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "setSize:", "Enriched Proteins:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "NES:", "Normalized Enrichment Score:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "p.adjust:", "Adj. P-Value (BH):")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "qvalue:", "Adj. P-Value (Q-Value):")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "leading_edge:", "Leading Edge Metrics:")
  colnames(gseFrame) <- str_replace_all(colnames(gseFrame), "core_enrichment:", "Core Enriched Proteins:")
  
  # For enriched proteins, evaluate whether they were signficantly up- or down-
  # regulated in associated ratio test
  indexG <- grep("Core Enriched Proteins:", colnames(gseFrame), fixed = T)
  indexP <- grep("Adj. P-Value", colnames(data), fixed = T)
  indexR <- grep("Abundance Ratio:", colnames(data), fixed = T)
  for (i in 1:length(indexG)) {
    
    ratioName <- trimws(str_replace(colnames(gseFrame)[indexG[i]], "Core Enriched Proteins:", ""))
    
    gseFrame[paste0("Num. Significant Upregulated: ", ratioName)] <- 0
    gseFrame[paste0("Num. Significant Downregulated: ", ratioName)] <- 0
    gseFrame[paste0("Significant Upregulated Accessions: ", ratioName)] <- ""
    gseFrame[paste0("Significant Downregulated Accessions: ", ratioName)] <- ""
    
    for (j in 1:nrow(gseFrame)) {
      tmp <- gseFrame[j, indexG[i]]
      tmp <- strsplit(tmp, "/", fixed = T)[[1]]
      
      frame <- data[match(tmp, data$Accession),]
      up <- frame[which(frame[[indexR[i]]] > 1 & frame[[indexP[i]]] <= 0.05),]
      down <- frame[which(frame[[indexR[i]]] < 1 & frame[[indexP[i]]] <= 0.05),]
      
      gseFrame[j, paste0("Significant Upregulated Accessions: ", ratioName)] <- paste0(up$Accession, collapse = "/")
      gseFrame[j, paste0("Significant Downregulated Accessions: ", ratioName)] <- paste0(down$Accession, collapse = "/")
      gseFrame[j, paste0("Num. Significant Upregulated: ", ratioName)] <- nrow(up)
      gseFrame[j, paste0("Num. Significant Downregulated: ", ratioName)] <- nrow(down)
    }
  }
  
  return(gseFrame)
}

#' Run ORA analysis using GO ontology database
#'
#' @param accession Array of significant accession ids
#' @param db Organism database
#' @param ont Ontology to search
#' @param universe Array of all detected protein accession ids
#'
#' @returns List of ORA results
#' 
runOraGO <- function(accession, db, ont, universe) {
  
  if (length(accession) > 1) {
    
    # Run over-representation analysis for a gene ontology database
    # Do not use any pvalue or qvalue cutoff
    # Universe is all accession detected in experiment
    ora <- enrichGO(accession, OrgDb = db, ont = ont, keyType = "UNIPROT",
                    universe = universe, pvalueCutoff = 1, qvalueCutoff = 1)
    resultORA <- ora@result
    
    # Add ratio name to pathway statistics columns
    names <- colnames(resultORA)
    names[-c(1:2)] <- paste0(names[-c(1:2)], ": ", ratioName)
    colnames(resultORA) <- names
    
  } else {
    
    # Make dummy result if no signficant proteins for ratio
    resultORA <- data.frame("ID" = "GO:0000000", "Description" = "Empty GO",
                            "GeneRatio" = NA, "BgRatio" = NA,
                            "pvalue" = NA, "p.adjust" = NA, "qvalue" = NA, "geneID" = NA, "Count" = NA)
    names <- colnames(resultORA)
    names[-c(1:2)] <- paste0(names[-c(1:2)], ": ", ratioName)
    colnames(resultORA) <- names
  }
  
  return(resultORA)
}

#' Run ORA analysis using KEGG database
#'
#' @param accession Array of significant accession ids
#' @param db Organism database
#' @param universe Background proteins detected in experiment
#'
#' @returns Raw pathway analysis results table
runOraKEGG <- function(accession, db, universe) {
  
  if (length(accession) > 1) {
    
    # Run over-representation analysis for a KEGG database
    # Do not use any pvalue or qvalue cutoff
    # Universe is all accession detected in experiment
    ora <- enrichKEGG(accession, organism = db, keyType = "uniprot",
                    universe = universe, pvalueCutoff = 1, qvalueCutoff = 1)
    resultORA <- ora@result
    
    # Add ratio name to pathway statistic columns
    names <- colnames(resultORA)
    names[-c(1:4)] <- paste0(names[-c(1:4)], ": ", ratioName)
    colnames(resultORA) <- names
    
  } else {
    
    # Make dummy variable if no significant accessions for ratio
    resultORA <- data.frame("category" = NA, "subcategory" = NA, ID = "GO:0000000",
                            "Description" = NA, "GeneRatio" = NA, "BgRatio" = NA,
                            "pvalue" = NA, "p.adjust" = NA, "qvalue" = NA, "geneID" = NA,
                            "Count" = NA)
    names <- colnames(resultORA)
    names[-c(1:4)] <- paste0(names[-c(1:4)], ": ", ratioName)
    colnames(resultORA) <- names
  }
  
  return(resultORA)
}

#' Run ORA analysis using REACTOME database
#'
#' @param accession Array of significant accession ids
#' @param organism Organism database code
#' @param db Gene ontology database
#' @param universe Background proteins detected in experiment
#'
#' @returns Raw pathway analysis results table
runOraREACTOME <- function(accession, organism, db, universe) {
  
  # Convert Uniprot accession to Entrez IDs
  entrez <- bitr(accession, fromType = "UNIPROT", toType = "ENTREZID", OrgDb = db)
  universe <- bitr(universe, fromType = "UNIPROT", toType = "ENTREZID", OrgDb = db)
  universe <- universe$ENTREZID
  
  # Run over-representation analysis for a KEGG database
  # Do not use any pvalue or qvalue cutoff
  # Universe is all accession detected in experiment
  x <- enrichPathway(gene=entrez$ENTREZID, organism = organism, universe = universe,
                     pvalueCutoff = 1.00, qvalueCutoff = 1.00, readable=FALSE)
  x <- x@result
  
  # Convert entrez ID to Uniprot Accessions
  geneID <- x$geneID
  for(i in 1:length(geneID)){
    tmp <- strsplit(geneID[i], "/", fixed = T)[[1]]
    for(j in 1:length(tmp)){
      tmp[j] <- entrez[which(entrez$ENTREZID == tmp[j]),"UNIPROT"]
    }
    tmp <- paste0(tmp, collapse = "/")
    geneID[i] <- tmp
  }
  x$geneID <- geneID
  
  # Associate ratio name with pathway statistics
  names <- colnames(x)
  names[-c(1:2)] <- paste0(names[-c(1:2)], ": ", ratioName)
  colnames(x) <- names
  
  return(x)
}


#' Run GSEA analysis using Gene Ontology database
#'
#' @param geneList Named vector of protein ranks
#' @param db Organism database
#' @param ont Ontology code
#'
#' @returns Raw data frame of pathway results
runGseGO <- function(geneList, db, ont) {
  
  # Run GSEA with specified seed
  # Do not filter results based on pvalue
  set.seed(6283)
  go <- gseGO(geneList, ont = ont, OrgDb = db, keyType = "UNIPROT",
              pvalueCutoff = 1.0, seed = TRUE, verbose = FALSE)
  go <- go@result
  
  # Format pathway statistics
  go$NES <- round(go$NES, 3)
  go$setSize <- str_count(go$core_enrichment, pattern = "/") + 1
  for(i in 1:nrow(go)) {
    go$leading_edge[i] <- paste0(go$leading_edge[i], ",rank=", go$rank[i])
  }
  
  # Associated ratio name with pathway statistics
  names <- colnames(go)
  names[-c(1:2)] <- paste0(names[-c(1:2)], ": ", ratioName)
  colnames(go) <- names
  
  return(go)
}

#' Run GSEA analysis using KEGG database
#'
#' @param geneList Named vector of protein ranks
#' @param db Organism database
#'
#' @returns Raw pathway analysis results table
runGseKEGG <- function(geneList, db) {
  
  # Conduct GSEA using specified seed
  # Do not filter results by pvalue
  set.seed(6283)
  go <- gseKEGG(geneList, organism = db, keyType = "uniprot",
              pvalueCutoff = 1.0, seed = TRUE, verbose = FALSE)
  go <- go@result
  
  # Format pathway statistics
  go$NES <- round(go$NES, 3)
  go$setSize <- str_count(go$core_enrichment, pattern = "/") + 1
  for(i in 1:nrow(go)) {
    go$leading_edge[i] <- paste0(go$leading_edge[i], ",rank=", go$rank[i])
    
  }
  
  # Associate ratio name with pathway statistics
  names <- colnames(go)
  names[-c(1:2)] <- paste0(names[-c(1:2)], ": ", ratioName)
  colnames(go) <- names
  
  return(go)
}

#' Run GSEA analysis using REACTOME database
#'
#' @param geneList Named vector of protein rankings
#' @param organism Organism code
#' @param db Organism gene ontology database
#'
#' @returns Raw pathway analysis results table
runGseREACTOME <- function(geneList, organism, db) {
  
  # Convert Uniprot accessions to entrez ID
  names <- names(geneList)
  entrez <- bitr(names, fromType = "UNIPROT", toType = "ENTREZID", OrgDb = db)
  
  eGeneList <- vector(mode = "numeric")
  for(i in 1:nrow(entrez)){
    
    tmp <- geneList[which(names == entrez$UNIPROT[i])]
    names(tmp) <- entrez$ENTREZID[i]
    eGeneList <- base::append(eGeneList, tmp)
  }
  
  if(any(duplicated(names(eGeneList)))) {
    n <- names(eGeneList[which(duplicated(names(eGeneList)))])
    for(i in 1:length(n)) {
      index <- which(names(eGeneList) == n[i])
      tmp <- eGeneList[index]
      index <- index[-which.max(abs(tmp))]
      eGeneList <- eGeneList[-index]
    }
  }
  
  # Run pathway analysis with specified seed
  # Do not filter results by pvalue
  set.seed(6283)
  gse <- gsePathway(eGeneList, organism = organism,
              pvalueCutoff = 1.0, seed = TRUE, verbose = FALSE)
  gse <- gse@result
  
  # Format pathway analysis statistics
  gse$NES <- round(gse$NES, 3)
  gse$setSize <- str_count(gse$core_enrichment, pattern = "/") + 1
  for(i in 1:nrow(gse)) {
    gse$leading_edge[i] <- paste0(gse$leading_edge[i], ",rank=", gse$rank[i])
    
  }
  
  # Associated ratio name with pathway statistics
  names <- colnames(gse)
  names[-c(1:2)] <- paste0(names[-c(1:2)], ": ", ratioName)
  colnames(gse) <- names
  
  return(gse)
}

# Command Line Arguments --------------------------------------------------

args = commandArgs(trailingOnly=TRUE)

if (length(args) != 5) {
  stop("Arguments not properly specified", call. = F)
}

importPath <- args[1]
exportPath <- args[2]
organism <- args[3]
pvalueCutoff <- args[4]
code <- args[5]

## Example reports -- use for testing
# importPath <- "D:\\projects\\ProteomeDiscoverer\\results\\ameeta_yang6906_19668_20240911_FAIMS_MS2_TMT12_customDB_Proteins.txt"
# exportPath <- "D:\\test.txt"
# organism <- "Mus musculus"
# pvalueCutoff <- 0.05
# code <- "ORA:GO-MF"

# Pathway Analysis --------------------------------------------------------

# Read and format user inputs
data <- readr::read_delim(importPath)
goDB <- get_goDB(organism)
keggCode <- get_keggCode(organism)
org <- get_organism(organism)

# Remove any contaminant proteins
if("Contaminant" %in% colnames(data)) {
  data <- data[which(data$Contaminant != "TRUE"),]
}

# Only process results derived from specifed organism
data <- data[grep(organism, data$Description, fixed = T),]

# Get relevant column indices
accessColumn <- which(colnames(data) == "Accession")
ratioColumns <- grep("Abundance Ratio:", colnames(data), fixed = F)
abundColumns <- grep("Abundances (Grouped):", colnames(data), fixed = T)
pavalColumns <- grep("Abundance Ratio Adj. P-Value:", colnames(data), fixed = T)
prvalColumns <- grep("Abundance Ratio P-Value:", colnames(data), fixed = T)

# For ratio in protein report, run pathway analysis
resultList <- list()
for (i in 1:length(ratioColumns)) {
  
  # Get ratio name
  ratioName <- colnames(data)[ratioColumns[i]]
  ratioName <- strsplit(ratioName, split = "Ratio:", fixed = T)[[1]][2]
  ratioName <- trimws(ratioName)
  
  # Get group names from ratio
  ratioA <- trimws(strsplit(ratioName, "/", fixed = T)[[1]][1])
  ratioA <- str_remove(ratioA, "\\(")
  ratioA <- str_remove(ratioA, "\\)")
  ratioB <- trimws(strsplit(ratioName, "/", fixed = T)[[1]][2])
  ratioB <- str_remove(ratioB, "\\(")
  ratioB <- str_remove(ratioB, "\\)")
  
  # Get abundance columns associated with groups
  abunAColumns <- which(colnames(data) == paste("Abundances (Grouped):", ratioA))
  abunBColumns <- which(colnames(data) == paste("Abundances (Grouped):", ratioB))

  # Filter relevant protein report columns
  df <- data[,c(accessColumn, ratioColumns[i], pavalColumns[i], prvalColumns[i], abunAColumns, abunBColumns)]
  colnames(df)[1:6] <- c("Accession", "foldChange", "adjPValue", "rawPValue", "abundanceA", "abundanceB")
  
  # Use -log pvalue for protein result ranking
  df$pvalue <- -log(df$rawPValue)
  
  # If any results represent presence-absence outcomes, adjust p-value
  # slightly to prevent proteins from overly influencing model fit
  index <- which(xor(is.na(df$abundanceA), is.na(df$abundanceB)))
  if(length(index) > 0) {
    tmp <- df[index,]
    minP <- max(df$pvalue[-index], na.rm = T) + 1
    df[index,]$pvalue <- minP
  }
  
  # For outcomes with identical p-values, adjust p-values slightly
  # by max protein abundance to prioritize readily detectable proteins
  df$Max <- apply(df[-c(1:4)], 1, function(x) max(x, na.rm = T))
  duplicate <- unique(df$pvalue[which(duplicated(df$pvalue))])
  for (dup in duplicate) {
    tmp <- df[which(df$pvalue == dup),]
    tmp$pvalue <- tmp$pvalue + rank(tmp$Max) * tmp$pvalue/100000
    df[which(df$pvalue == dup),] <- tmp
  }
  
  # Background universe reflects all detectable proteins
  # Analyze ORA outcomes using statistically significant outcomes
  universe <- df$Accession
  accession <- df$Accession[which(df$adjPValue <= pvalueCutoff)]
  
  # Format ranked gene list (protein list)
  # Protein ranking: -log(pvalue) * sign of fold-change
  df <- df[which(!is.na(df$pvalue)),]
  geneList <- sign(log2(df$foldChange)) * df$pvalue
  names(geneList) <- as.character(df$Accession)
  geneList <- sort(geneList, decreasing = T)
  
  # Submit for pathway analysis by code
  switch(code,
         "ORA:GO-BP"={result = runOraGO(accession, goDB, ont = "BP", universe)},
         "ORA:GO-MF"={result = runOraGO(accession, goDB, ont = "MF", universe)},
         "ORA:GO-CC"={result = runOraGO(accession, goDB, ont = "CC", universe)},
         "ORA:KEGG"={result = runOraKEGG(accession, keggCode, universe)},
         "ORA:REACTOME"={result = runOraREACTOME(accession, org, goDB, universe)},
         "GSE:GO-BP"={result = runGseGO(geneList, goDB, ont = "BP")},
         "GSE:GO-MF"={result = runGseGO(geneList, goDB, ont = "MF")},
         "GSE:GO-CC"={result = runGseGO(geneList, goDB, ont = "CC")},
         "GSE:KEGG"={result = runGseKEGG(geneList, keggCode)},
         "GSE:REACTOME"={result = runGseREACTOME(geneList, org, goDB)})
  
  resultList[[i]] <- result
}

# For result list, merge and format into final report
switch(code,
       "ORA:GO-BP"={result = format_oraGO(resultList, data, goDB)},
       "ORA:GO-MF"={result = format_oraGO(resultList, data, goDB)},
       "ORA:GO-CC"={result = format_oraGO(resultList, data, goDB)},
       "ORA:KEGG"={result = format_oraKegg(resultList, data, keggCode)},
       "ORA:REACTOME"={result = format_oraREACTOME(resultList, data, org, goDB)},
       "GSE:GO-BP"={result = format_gseGO(resultList, data, goDB)},
       "GSE:GO-MF"={result = format_gseGO(resultList, data, goDB)},
       "GSE:GO-CC"={result = format_gseGO(resultList, data, goDB)},
       "GSE:KEGG"={result = format_gseKegg(resultList, data, keggCode)},
       "GSE:REACTOME"={result = format_gseREACTOME(resultList, data, org, goDB)})

# Write results as tab-delimited text file
write.table(result, exportPath, row.names = F, na = "", sep = "\t")

# End of Script -----------------------------------------------------------

