import java.util.*;

public class Hub extends AbstractNode {
    private HashMap<String,fichierDemande> fichierDemande; // Liste de <String filename, nbr de demande> s'actualise à chaque appel de read (TODO)
    private HashMap<String,fichierDemande> fichiersHub; //
    public Hub(int id,int nbrFichierMax, Time time,Integer limit)
    {
        this.chargeReseauxRead=0;
        this.chargeReseauxIncrease=0;
        this.chargeReseauxWrite=0;
        this.chargeReseauxStore=0;
        this.chargeReseauxReStore=0;
        this.id=id;
        this.voisins =new ArrayList<>();
        this.fichierDemande =new HashMap<>();
        this.nbrFichierMax=nbrFichierMax;
        this.nbrFichier=0;
        this.fichiersHub =new HashMap<>();
        this.cache=new cache(limit,time);
     /*   this.cachingProvider = Caching.getCachingProvider();
        this.cacheManager = cachingProvider.getCacheManager();
        this.config
                = new MutableConfiguration<>();
        cache = cacheManager
                .createCache("simpleCache"+this.getId(), config);*/

        //cache.close();
    }
    private void addFichierDemande(String filename)
    {
        if(this.fichiersHub.containsKey(filename))
        {
            this.fichiersHub.get(filename).addDemande();
            return;
        }
        if(!this.fichierDemande.containsKey(filename))
        {
            fichierDemande.put(filename,new fichierDemande(0,filename));
        }
        fichierDemande.get(filename).addDemande();

    }
    @Override
    public Message read(String filename, int replique) {
        List<Integer> presence = new ArrayList<>();
        List<Message> listeRetour = new ArrayList<>();
        chargeReseauxRead+=1;
        addFichierDemande(filename);
        int r=0;
        if(cache.containsKey(filename))
        {
            List<Integer> leCache = cache.get(filename);
            int i=0;
            while(i<leCache.size() && r<replique)
            {
                Message retour = readTo(new Message(1,this.getId(),filename,leCache.get(i),this.getDemande(filename),0));
                if(retour.msgType==1)
                {
                    listeRetour.add(retour);
                    r++;
                    presence.add(leCache.get(i));
                }
                i++;
            }
        }
        int i=0;
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        if(cache.containsKey(filename))
        {
            pref.removeAll(cache.get(filename)); // TODO opti
        }
        while(r<replique)
        {
            if(i>=pref.size())
            {
                // TODO same as store
                System.out.println("Can't find replica");
                return new Message(0,null,null,-1,-1);
            }
            //if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to give "+filename);
            Message retour = readTo(new Message(1,this.getId(),filename,pref.get(i),this.getDemande(filename),0));

            if (retour.msgType==1)
            {
                listeRetour.add(retour);
                //  System.out.println("Hub "+this.getId()+" Succesfuly found a replica ");
                presence.add(pref.get(i));
                r++;
                // TODO get the replica and check it's good version
            }
            i++;
        }

        if(fichierDemande.containsKey(filename)) {
            if (true){//(nbrFichier<nbrFichierMax||pireFichierDemande()==null || (pireFichierDemande().getDemande() < fichierDemande.get(filename).getDemande())) {
                List<Integer> newPref;
                         newPref = newPref(listeRetour.stream().map(f->f.hubIDemande).toList());


                    List<Integer> oldPresence = presence;

                    List<Integer> actualIndex = getActualIndex(newPref,oldPresence);
                System.out.println("newpref "+newPref);
                    int indiceMax= getMaxIndex(actualIndex);
                    if(newPref.indexOf(this.getId())<actualIndex.get(indiceMax))
                    {
                        if(this.removeTo(new Message(2,filename,oldPresence.get(indiceMax),this.fichierDemande.get(filename).getDemande())).msgType==1) {
                            this.take(new Message(2, filename));
                            presence.remove(Integer.valueOf(oldPresence.get(indiceMax)));
                            presence.add(this.getId());
                        }
                    }
                }
          }
        if(cache.containsKey(filename))
        {
            cache.remove(filename);
        }
        cache.put(filename,presence);
        // System.out.println("Hub "+this.getId()+" found "+filename+" at hubs : "+findWhere);
        return new Message(1,null,null,-1,-1); // oué on devrai retourner le contenu mais flemme, j'ai mal gerer les msg et il est perdu en chemin  ¯\_(ツ)_/¯
    }
    public  int getMaxIndex(List<Integer> integerList) {
        int maxIndex = 0;
        int maxValue = Integer.MIN_VALUE;

        for (int i = 0; i < integerList.size(); i++) {
            int currentValue = integerList.get(i);
            if (currentValue > maxValue) {
                maxValue = currentValue;
                maxIndex = i;
            }
        }

        return maxIndex;
    }
    public List<Integer> getActualIndex(List<Integer> newPref, List<Integer> oldPresence) {
        List<Integer> actualIndex = new ArrayList<>();

        for (int element : oldPresence) {
            int index = newPref.indexOf(element);
            actualIndex.add(index);
        }

        return actualIndex;
    }
    private List<Integer> newPref(List<HashMap<Integer, Integer>> hubIdemande) {

      ///  System.out.println("-------------");
        HashMap<Integer,Integer> hubIDemandeActual = new HashMap<>();
        for(HashMap<Integer,Integer> old : hubIdemande)
        {
            for (Integer key : old.keySet()) {
                int demande = old.get(key);
                if(hubIDemandeActual.containsKey(key))
                {
                    if(hubIDemandeActual.get(key)<demande)
                    {
                        hubIDemandeActual.put(key,demande);
                    }
                }else
                {
                    hubIDemandeActual.put(key,demande);
                }
            }
        }

        List<Double> newTopo=new ArrayList<>();
        for(int i=0;i<topologyMoyenneGlobal.size();i++)
        {
            int sum=0;
            //for(int j = 0 ; j < topologyMoyenneGlobal.size();j++)
            //{
            //System.out.println("--");
                for(Integer key : hubIDemandeActual.keySet())
                {
                    int demande = hubIDemandeActual.get(key);
            //        System.out.println("Hub "+key+" demande : "+demande);
              //      System.out.println(djkstraHubI.get(i).get(key)+" * "+demande);
                    sum+=demande*djkstraHubI.get(i).get(key);
                }
          //  System.out.println(i+" "+sum);
                newTopo.add((double)sum);
            //}
        }
        //System.out.println("newtopo "+newTopo);
        //System.out.println(djkstraHubI);
        return getAsIndexList(newTopo);
    }
    private List<Integer> getAsIndexList(List<Double> doubleList) {
        List<Integer> indexList = new ArrayList<>();

        // Créer une liste d'indices de 0 à size - 1
        for (int i = 0; i < doubleList.size(); i++) {
            indexList.add(i);
        }
        // Trier la liste d'indices en utilisant les valeurs correspondantes de la liste de doubles
        Collections.sort(indexList, Comparator.comparing(doubleList::get));

        return indexList;
    }
    @Override
    public Message readTo(Message msg) {
        int hubId= msg.destinataire;
        String filename= msg.nomFichier;
        if(this.getId()==hubId)
        {
            return give(msg);
        }
        msg.distance+=1;
        chargeReseauxRead+=1; //1724 4556
        //System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        //return this.routingTable.get(hubId).readTo(filename,hubId);
        Message retMsg = this.routingTable.get(hubId).readTo(msg);
        if(retMsg.msgType==1) // si un de mes voisins dit avoir un fichier, autant le retenir dans mon cache
        {
            //   if(this.routingTable.get(hubId).getId()==hubId){
            if(this.cache.containsKey(filename))
            {
                this.cache.get(filename).add(hubId);
            }
            else
            {
                this.cache.put(filename,new ArrayList<>(hubId));
            }
            return retMsg;
            // }
            //return true;
        }
        return new Message(0); //false
    }

    @Override
    public Message give(Message msg) {
        String filename = msg.nomFichier;
        if(fichiersHub.containsKey(filename)) {
            this.fichiersHub.get(filename).addHubIdemande(msg.source);
            //  System.out.println("Hub "+this.getId()+" lecture de "+filename+" sur la node "+this.fichierNode.get(filename).getNode().getId());
            return new Message(1, this.getId(), filename, msg.source, msg.distance, fichiersHub.get(filename).getHubIDemande());
        }
            return new Message(0);
    }

    @Override
    public Message removeTo(Message msg) {
        int destinaire = msg.destinataire;
        if(destinaire==this.getId())
        {
            if(msg.msgType==2 || this.fichiersHub.get(msg.nomFichier).getDemande()<msg.poidsFichier)
            {
                this.fichierDemande.put(msg.nomFichier,this.fichiersHub.get(msg.nomFichier));
                this.fichiersHub.remove(msg.nomFichier);
                this.nbrFichier--;
                return new Message(1);
            }
            return new Message(0);
        }
        return this.routingTable.get(destinaire).removeTo(msg);
    }



    @Override
    public Message take(Message msg) {
        String filename = msg.nomFichier;
        if(msg.msgType==2)
        {
            this.nbrFichier++;
            int demande=0;
            if(this.fichierDemande.containsKey(filename))
            {
                demande=this.fichierDemande.get(filename).getDemande();
                this.fichierDemande.remove(filename);
            }
            this.fichiersHub.put(filename,new fichierDemande(demande,filename));
           // this.fichiersHub.get(filename).lock(false); // pas le droit d'y toucher
            return new Message(1,filename,msg.contenuFichier,msg.destinataire,msg.poidsFichier);
        }
        if(this.nbrFichier<nbrFichierMax && !this.fichiersHub.containsKey(filename))
        {
            //  System.out.println("Hub "+this.getId()+": store in node");
            this.nbrFichier++;
            int demande = 0;
            if(this.fichierDemande.containsKey(filename))
            {
                demande = this.fichierDemande.get(filename).getDemande();
                this.fichierDemande.remove(filename);
            }
            this.fichiersHub.put(filename,new fichierDemande(demande,filename));
            if(msg.msgType==2){

            }
            return new Message(1,filename,msg.contenuFichier,msg.destinataire, msg.poidsFichier);
        }
        return new Message(0);
    }


    @Override
    public Message write(Message msg, int replique) {
        this.chargeReseauxWrite+=1;
        String filename=msg.nomFichier;
        List<Integer> presence = new ArrayList<>();
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        //System.out.println("/!\\Hub "+this.getId()+" rewriting "+filename+" by "+newContenu);
        int r=0;
        if(cache.containsKey(filename))
        {
            pref.removeAll(cache.get(filename));
            List<Integer> leCache= cache.get(filename);
            int i =0;
            while(i<leCache.size() && r<replique)
            {
                if(writeTo(new Message(1,filename,msg.contenuFichier,leCache.get(i),-1)).msgType==1)
                {
                    r++;
                    presence.add(leCache.get(i));
                }
                i++;
            }
        }
        int i=0;
        while(r<replique)
        {
            if(i>=pref.size())
            {
                System.out.println("Hub "+this.getId()+ " Write :Can't find enought replica to write "+filename+" "+r+" / "+replique);
                return new Message(0);
            }
            // if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            //System.out.println("Hubs "+this.getId()+" Write to "+filename+" "+newContenu+" "+pref.get(i));
            if(writeTo(new Message(1,filename,msg.contenuFichier,pref.get(i),-1)).msgType==1)
            {
                r++;
                presence.add(pref.get(i));
                // TODO get the replica and check it's good version
            }
            i++;
        }
        if(cache.containsKey(filename))
        {
            cache.remove(filename);
        }
        cache.put(filename,presence);
        return new Message(1,null,null,-1,-1);
    }

    @Override
    public Message writeTo(Message msg) {
        String filename=msg.nomFichier;
        String newContenu=msg.contenuFichier;
        int h = msg.destinataire;
        if(fichiersHub.containsKey(filename))
        {
          //  System.out.println("writeIn");
            return this.writeIn(msg);
        }
        if(h==this.getId())
        {
           // System.out.println("falseWrite");
            return new Message(0);
        }
        this.chargeReseauxWrite+=1;
        //      System.out.println("Hubs "+this.getId()+" need to "+h);
        //    System.out.println("Hub "+this.getId()+" go by "+routingTable.get(h).getId());
        //  return this.routingTable.get(h).writeTo(filename,newContenu,h);
      //  System.out.println("msgRetour");
        Message msgRetour = this.routingTable.get(h).writeTo(msg);
        if(msgRetour.msgType==1) // si un de mes voisins dit avoir un fichier, autant le retenir dans mon cache
        {
            //if(this.routingTable.get(h).getId()==h){
            if(this.cache.containsKey(filename))
            {
                this.cache.get(filename).add(h);
            }
            else
            {
                this.cache.put(filename,new ArrayList<>(h));
            }
            return msgRetour;
            //}
            //    return true;
        }
        return msgRetour;
    }
    private Message writeIn(Message msg)
    {
        this.fichiersHub.get(msg.nomFichier).contenu= msg.contenuFichier;
        return new Message(1);
    }


    private fichierDemande pireFichierDemande() {
        String keyWithSmallestNumber = null;
        int smallestNumber = Integer.MAX_VALUE;
        for (Map.Entry<String, fichierDemande> entry : fichiersHub.entrySet()) {
            if(entry.getValue().isLibre()) {
                int number = entry.getValue().getDemande();
                if (number < smallestNumber) {
                    smallestNumber = number;
                    keyWithSmallestNumber = entry.getKey();
                }
            }
        }if(keyWithSmallestNumber!=null) {
            return fichiersHub.get(keyWithSmallestNumber);
        }
        return null;
    }

    @Override

    public  String getFichierDemande() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hubs "+this.getId()+" :\n");
        for (String key : this.fichiersHub.keySet()) {
            fichierDemande fd = this.fichiersHub.get(key);
            String nom = fd.getNom();
            stringBuilder.append("Clé : ").append(key).append(" in ").append(this.fichiersHub.get(key).getDemande());
            if(fichiersHub.get(key).isLibre())
            {
                stringBuilder.append(" libre \n");
            }
            else
            {
                stringBuilder.append(" lock \n");
            }
        }
        for (String key : this.fichierDemande.keySet()) {
            fichierDemande fd = this.fichierDemande.get(key);
            String nom = fd.getNom();
            stringBuilder.append("Clé : ").append(key).append(", Nom : ").append(this.fichierDemande.get(fd.getNom()).calculPoids()).append("-").append(nom).append("\n");
        }

        return stringBuilder.toString();
    }
    public int getDemande(String filename)
    {
        if(this.fichierDemande.containsKey(filename))
        {
            return this.fichierDemande.get(filename).getDemande();
        }
        return 0;
    }
}