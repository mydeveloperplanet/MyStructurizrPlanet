workspace {

    model {
        customer = person "Customer" "The customer of our webshop"
        administrator = person "Administrator" "The administrator of the webshop"
        globalPayment = softwareSystem "Global Payment" "Used for all banking transactions"

        myWebshop = softwareSystem "My Webshop" "Our beautiful webshop"

        customer -> myWebshop "Uses"
        administrator -> myWebshop "Uses"
        myWebshop -> globalPayment "Uses"
    }

    views {
        systemContext myWebshop "MyWebshopSystemContextView" {
            include *
            autolayout
        }
       theme default
    }

}