public class Share {
    //A class to store the share properties and handle its basic calculations. I think all methods are self-explanatory.
    
    private String shareName;
    private double sharePrice;
    private double numberOfShares;
    private double indexWeightPct;
    private double indexValue;

    public Share() {
        super();
    }

    public Share(String name, double price, double number) {
        this.shareName=name;
        this.sharePrice=price;
        this.numberOfShares=number;
        this.indexValue=this.numberOfShares*this.sharePrice;
    }
    
    public void SetShareName(String name) {
        this.shareName=name;
    }
    public String GetShareName() {
        return this.shareName;
    }

    public void SetSharePrice(double price) {
        this.sharePrice=price;
    }
    public double SetDiffPrice(double price) {
        double r=(this.sharePrice-price)*this.numberOfShares;
        this.sharePrice=price;       
        this.indexValue=this.numberOfShares*this.sharePrice;
        return r;
    }
    public double GetSharePrice() {
        return this.sharePrice;
    }

    public void SetNumberOfShares(double number) {
        this.numberOfShares=number;
    }
    public double GetNumberOfShares() {
        return this.numberOfShares;
    }

    public void SetIndexWeightPct(double wct) {
        this.indexWeightPct=wct;
    }
    public void CalcWeightPct(double totalValue) {
        this.indexWeightPct=100*this.indexValue/totalValue;
    }
    public double GetIndexWeightPct() {
        return this.indexWeightPct;
    }

    public void SetIndexValue(double value) {
        this.indexValue=value;
    }
    public double GetIndexValue() {
        return this.indexValue;
    }
    public void MultIndexValue(double factor) {
        this.indexValue*=factor;
        this.indexWeightPct*=factor;
        this.numberOfShares*=factor;
    }

    public double CalculateIndex() {
        this.indexValue=this.numberOfShares*this.sharePrice;
        return this.indexValue;
    } 
}